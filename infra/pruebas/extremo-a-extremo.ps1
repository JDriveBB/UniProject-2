param([switch]$SinLevantar)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function dc {
    $input | docker compose --env-file .env -f infra/docker-compose.yml @args
}

function Sql([string]$consulta) {
    $salida = $consulta | dc exec -T base-de-datos psql -U postgres -d uniproject -At -v ON_ERROR_STOP=1
    if ($null -eq $salida) {
        return ""
    }
    return ($salida -join "`n").Trim()
}

function LeerEnv([string]$clave) {
    if (-not (Test-Path ".env")) { return "" }
    foreach ($linea in Get-Content ".env") {
        $linea = $linea.Trim()
        if ($linea.StartsWith("#") -or $linea -eq "") { continue }
        if ($linea -match ('^' + [regex]::Escape($clave) + '=(.*)$')) {
            return $matches[1].Trim()
        }
    }
    return ""
}

function Esperar([scriptblock]$condicion, [int]$segundos) {
    $limite = [DateTime]::UtcNow.AddSeconds($segundos)
    while ([DateTime]::UtcNow -lt $limite) {
        try {
            if (& $condicion) {
                return $true
            }
        } catch {
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Post([string]$ruta, [string]$json, [bool]$conToken) {
    $tempFile = [System.IO.Path]::GetTempFileName()
    try {
        Set-Content -Path $tempFile -Value $json -NoNewline -Encoding ascii
        $argsCurl = @("-s", "-o", "NUL", "-w", "%{http_code}", "-X", "POST", "-H", "Content-Type: application/json")
        if ($conToken) {
            $token = LeerEnv "UNIPROJECT_TOKEN_INTERNO"
            $argsCurl += "-H"
            $argsCurl += "X-UniProject-Token-Interno: $token"
        }
        $argsCurl += "--data-binary"
        $argsCurl += "@$tempFile"
        $argsCurl += "http://localhost:8080$ruta"
        $codigo = & curl.exe @argsCurl
        return "$codigo".Trim()
    } finally {
        if (Test-Path $tempFile) {
            Remove-Item -Force $tempFile
        }
    }
}

function ContarCola([string]$cola) {
    $salida = dc exec rabbitmq rabbitmqctl -q --no-table-headers list_queues name messages
    foreach ($linea in ($salida -split "`r?`n")) {
        $partes = $linea.Trim() -split '\s+'
        if ($partes.Count -ge 2 -and $partes[0] -eq $cola) {
            return [int]$partes[1]
        }
    }
    throw "La cola $cola no aparece en list_queues"
}

function Marca {
    $marca = Sql "select floor(extract(epoch from clock_timestamp()))::bigint"
    if ($marca -notmatch '^\d+$') {
        throw "Marca de tiempo invalida: $marca"
    }
    return $marca
}

function LogsAgente([string]$desde) {
    return ((dc logs --since $desde agente) -join "`n")
}

$U1 = 9001
$U2 = 9002
$CHAT1 = 777000001
$CHAT2 = 777000002
$COD_VIGENTE = "E2E-vigente-00000000000000000001"
$COD_VENCIDO = "E2E-vencido-00000000000000000001"
$COD_INEXIST = "E2E-inexist-00000000000000000001"
$SUFIJO = [guid]::NewGuid().ToString('N').Substring(0, 12)

$EVENTO_HITO_ENVIADO = '{"specversion":"1.0","id":"<ID>","source":"/uniproject/nucleo","type":"hito.enviado","time":"2026-10-24T19:05:00Z","datacontenttype":"application/json","data":{"proyecto":{"id":12,"nombre":"Proyecto final"},"equipo":{"id":4,"nombre":"Equipo 4"},"hito":{"id":7,"nombre":"Entrega de diagramas"},"fechaEnvio":"2026-10-24T19:05:00Z","estudiante":{"id":100,"nombre":"Estudiante C"},"docente":{"id":200,"nombre":"Docente D","chatId":<CHAT>}}}'

$okCount = 0

# E1 Arranque
try {
    $botToken = LeerEnv "TELEGRAM_BOT_TOKEN"
    if ($botToken -ne "") {
        Write-Host "E1 FALLO: deja TELEGRAM_BOT_TOKEN vacio para E1-E7"
        exit 1
    }

    if (-not $SinLevantar) {
        dc up -d --build
    }

    $arrancaron = Esperar {
        $logNucleo = (dc logs nucleo) -join "`n"
        $logAgente = (dc logs agente) -join "`n"
        ($logNucleo -match "Started NucleoAplicacion") -and ($logAgente -match "Started AgenteAplicacion")
    } 180

    if (-not $arrancaron) {
        Write-Host "E1 FALLO: nucleo o agente no arrancaron en 180 s"
    } else {
        $tablasCount = Sql "select count(*) from information_schema.tables where table_schema='public' and table_name in ('estado_riesgo_tarea','alerta_inactividad')"
        $logAgente = (dc logs agente) -join "`n"
        $tieneAvisoToken = $logAgente -match "TELEGRAM_BOT_TOKEN vac.o"

        if ($tablasCount -eq "2" -and $tieneAvisoToken) {
            $okCount++
            Write-Host "E1 OK"
        } else {
            Write-Host "E1 FALLO: tablasCount=$tablasCount (esperado 2), tieneAvisoToken=$tieneAvisoToken (esperado True)"
        }
    }
} catch {
    Write-Host "E1 FALLO: $($_.Exception.Message)"
}

# E2 Seguridad
try {
    $r1 = Post "/interno/telegram/vinculaciones" "{`"codigo`":`"$COD_INEXIST`",`"chatId`":$CHAT1}" $false
    $r2 = Post "/interno/telegram/vinculaciones" "{`"codigo`":`"$COD_INEXIST`",`"chatId`":$CHAT1}" $true
    $r3 = Post "/interno/telegram/vinculaciones" '{"codigo":"abc","chatId":1}' $true
    $r4 = (& curl.exe -s -o NUL -w "%{http_code}" "http://localhost:8080/").Trim()
    $r5 = (& curl.exe -s -o NUL -w "%{http_code}" "http://localhost:8080/actuator/health").Trim()

    if ($r1 -eq "401" -and $r2 -eq "404" -and $r3 -eq "400" -and $r4 -eq "401" -and $r5 -eq "200") {
        $okCount++
        Write-Host "E2 OK"
    } else {
        Write-Host "E2 FALLO: esperado [401, 404, 400, 401, 200] pero salio [$r1, $r2, $r3, $r4, $r5]"
    }
} catch {
    Write-Host "E2 FALLO: $($_.Exception.Message)"
}

# E3 Vinculacion
try {
    $sqlE3 = "INSERT INTO usuario (id_usuario) VALUES ($U1), ($U2) ON CONFLICT (id_usuario) DO UPDATE SET telegram_chat_id = NULL; DELETE FROM codigo_vinculacion WHERE codigo LIKE 'E2E-%'; INSERT INTO codigo_vinculacion (codigo, usuario_id, creado_en, vence_en) VALUES ('$COD_VIGENTE', $U1, now(), now() + interval '15 minutes'), ('$COD_VENCIDO', $U2, now() - interval '1 hour', now() - interval '30 minutes');"
    Sql $sqlE3

    $rVigente = Post "/interno/telegram/vinculaciones" "{`"codigo`":`"$COD_VIGENTE`",`"chatId`":$CHAT1}" $true
    $rVigenteRep = Post "/interno/telegram/vinculaciones" "{`"codigo`":`"$COD_VIGENTE`",`"chatId`":$CHAT1}" $true
    $rVencido = Post "/interno/telegram/vinculaciones" "{`"codigo`":`"$COD_VENCIDO`",`"chatId`":$CHAT2}" $true

    $chat9001 = Sql "select telegram_chat_id from usuario where id_usuario = $U1"
    $chat9002 = Sql "select coalesce(telegram_chat_id::text, '') from usuario where id_usuario = $U2"

    if ($rVigente -eq "204" -and $rVigenteRep -eq "410" -and $rVencido -eq "410" -and $chat9001 -eq "$CHAT1" -and $chat9002 -eq "") {
        $okCount++
        Write-Host "E3 OK"
    } else {
        Write-Host "E3 FALLO: esperado [204, 410, 410, $CHAT1, ''] pero salio [$rVigente, $rVigenteRep, $rVencido, $chat9001, $chat9002]"
    }
} catch {
    Write-Host "E3 FALLO: $($_.Exception.Message)"
}

# E4 Bloqueo (403)
try {
    $rBloqueo1 = Post "/interno/telegram/bloqueos" "{`"chatId`":$CHAT1}" $true
    $rBloqueo2 = Post "/interno/telegram/bloqueos" "{`"chatId`":$CHAT1}" $true
    $chat9001Bloqueo = Sql "select coalesce(telegram_chat_id::text, '') from usuario where id_usuario = $U1"

    if ($rBloqueo1 -eq "204" -and $rBloqueo2 -eq "204" -and $chat9001Bloqueo -eq "") {
        $okCount++
        Write-Host "E4 OK"
    } else {
        Write-Host "E4 FALLO: esperado [204, 204, ''] pero salio [$rBloqueo1, $rBloqueo2, $chat9001Bloqueo]"
    }
} catch {
    Write-Host "E4 FALLO: $($_.Exception.Message)"
}

# E5 Outbox -> RabbitMQ -> agente
try {
    $idE5 = "0e2e0005-0000-4000-8000-$SUFIJO"
    $desdeE5 = Marca

    $jsonE5 = $EVENTO_HITO_ENVIADO.Replace('<ID>', $idE5).Replace('<CHAT>', 'null')
    Sql "INSERT INTO evento_saliente (id, tipo, carga) VALUES ('$idE5', 'hito.enviado', '$jsonE5'::jsonb);"

    $publicadoE5 = Esperar {
        (Sql "select publicado_en is not null from evento_saliente where id = '$idE5'") -eq "t"
    } 15

    $logAgenteE5 = Esperar {
        (LogsAgente $desdeE5) -match "Evento $idE5 \(hito.enviado\) sin destinatarios con Telegram"
    } 15

    $procesadosE5 = Sql "select count(*) from agente.evento_procesado where evento_id = '$idE5'"

    if ($publicadoE5 -and $logAgenteE5 -and $procesadosE5 -eq "0") {
        $okCount++
        Write-Host "E5 OK"
    } else {
        Write-Host "E5 FALLO: publicado=$publicadoE5 (esperado True), logAgente=$logAgenteE5 (esperado True), procesados=$procesadosE5 (esperado 0)"
    }
} catch {
    Write-Host "E5 FALLO: $($_.Exception.Message)"
}

# E6 Evento invalido -> fallidos
try {
    $colaFallidos = "notificaciones.telegram.fallidos"
    $antes = ContarCola $colaFallidos
    $desdeE6 = Marca
    $rmqUser = LeerEnv "RABBITMQ_ADMIN_USER"
    $rmqPass = LeerEnv "RABBITMQ_ADMIN_PASSWORD"

    $payloadInvalido = '{"properties":{"delivery_mode":2,"content_type":"application/cloudevents+json"},"routing_key":"hito.enviado","payload":"{\"specversion\":\"1.0\"}","payload_encoding":"string"}'
    $tempE6 = [System.IO.Path]::GetTempFileName()
    try {
        Set-Content -Path $tempE6 -Value $payloadInvalido -NoNewline -Encoding ascii
        $respE6 = & curl.exe -s -u "$($rmqUser):$($rmqPass)" -H "content-type: application/json" -X POST --data-binary "@$tempE6" "http://localhost:15672/api/exchanges/%2F/uniproject.eventos/publish"
    } finally {
        if (Test-Path $tempE6) { Remove-Item -Force $tempE6 }
    }

    $routedOk = $respE6 -match '"routed":true'

    $llegoFallidos = Esperar {
        (ContarCola $colaFallidos) -eq ($antes + 1)
    } 15

    $logInvalido = Esperar {
        (LogsAgente $desdeE6) -match "Evento inv.lido, va a fallidos"
    } 15

    & curl.exe -s -u "$($rmqUser):$($rmqPass)" -X DELETE "http://localhost:15672/api/queues/%2F/notificaciones.telegram.fallidos/contents" | Out-Null

    if ($routedOk -and $llegoFallidos -and $logInvalido) {
        $okCount++
        Write-Host "E6 OK"
    } else {
        Write-Host "E6 FALLO: routedOk=$routedOk (esperado True), llegoFallidos=$llegoFallidos (esperado True), logInvalido=$logInvalido (esperado True)"
    }
} catch {
    Write-Host "E6 FALLO: $($_.Exception.Message)"
}

# E7 Error temporal y reintento diferido
try {
    $idE7 = "0e2e0007-0000-4000-8000-$SUFIJO"
    $desdeE7 = Marca

    $jsonE7 = $EVENTO_HITO_ENVIADO.Replace('<ID>', $idE7).Replace('<CHAT>', "$CHAT2")
    Sql "INSERT INTO evento_saliente (id, tipo, carga) VALUES ('$idE7', 'hito.enviado', '$jsonE7'::jsonb);"

    $reintentado = Esperar {
        $logA7 = LogsAgente $desdeE7
        $matches7 = [regex]::Matches($logA7, "Error temporal con el evento $idE7")
        $matches7.Count -ge 2
    } 45

    $fallidosCount7 = ContarCola "notificaciones.telegram.fallidos"
    $procesadosCount7 = Sql "select count(*) from agente.evento_procesado where evento_id = '$idE7'"

    $rmqUser = LeerEnv "RABBITMQ_ADMIN_USER"
    $rmqPass = LeerEnv "RABBITMQ_ADMIN_PASSWORD"
    & curl.exe -s -u "$($rmqUser):$($rmqPass)" -X DELETE "http://localhost:15672/api/queues/%2F/notificaciones.telegram/contents" | Out-Null

    $pendientes = ContarCola "notificaciones.telegram"
    if ($pendientes -gt 0) {
        Write-Host "AVISO: quedan mensajes diferidos; iran a fallidos en unos 30 min"
    }

    if ($reintentado -and $fallidosCount7 -eq 0 -and $procesadosCount7 -eq "0") {
        $okCount++
        Write-Host "E7 OK"
    } else {
        Write-Host "E7 FALLO: reintentado=$reintentado (esperado >= 2 errores temporales), fallidos=$fallidosCount7 (esperado 0), procesados=$procesadosCount7 (esperado 0)"
    }
} catch {
    Write-Host "E7 FALLO: $($_.Exception.Message)"
}

Write-Host "RESULTADO: $okCount/7 pasos OK"

if ($okCount -eq 7) {
    exit 0
} else {
    exit 1
}

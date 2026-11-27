param([switch]$Levantar)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Pruebas de extremo a extremo del dominio (bloques 24 y 28): D1-D9 y D11 por HTTP contra el nucleo en
# http://localhost:8080, con cookies y CSRF como el SPA. No lee .env (solo docker compose lo usa para
# entrar a la BD) y no depende de Telegram: los usuarios de la prueba no tienen chat vinculado.
# Se corre desde la raiz del repositorio: .\infra\pruebas\dominio.ps1 [-Levantar]

$BASE = "http://localhost:8080"
$SUFIJO = [guid]::NewGuid().ToString('N').Substring(0, 10)
$CLAVE = "ClaveE2E-$SUFIJO"
$TMP = Join-Path ([System.IO.Path]::GetTempPath()) ("uniproject-dominio-" + $SUFIJO)
New-Item -ItemType Directory -Path $TMP | Out-Null
$UTF8 = New-Object System.Text.UTF8Encoding($false)
$TOTAL = 10
$ok = 0

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

function Correo([string]$quien) {
    return "e2e.$quien.$SUFIJO@prueba.invalid"
}

function Fecha([int]$dias) {
    return (Get-Date).AddDays($dias).ToString("yyyy-MM-dd", [System.Globalization.CultureInfo]::InvariantCulture)
}

function ComoJson($objeto) {
    return ($objeto | ConvertTo-Json -Compress -Depth 6)
}

function Tarro([string]$actor) {
    $ruta = Join-Path $TMP "$actor.cookies"
    if (-not (Test-Path $ruta)) {
        [System.IO.File]::WriteAllText($ruta, "", $UTF8)
    }
    return $ruta
}

function Xsrf([string]$actor) {
    foreach ($linea in [System.IO.File]::ReadAllLines((Tarro $actor))) {
        $partes = $linea -split "`t"
        if ($partes.Count -ge 7 -and $partes[5] -eq "XSRF-TOKEN") {
            return $partes[6].Trim()
        }
    }
    return ""
}

function Api([string]$actor, [string]$metodo, [string]$ruta, [string]$json = "", [switch]$SinCsrf) {
    $tarro = Tarro $actor
    $salida = Join-Path $TMP "respuesta.bin"
    if (Test-Path $salida) {
        Remove-Item -Force $salida
    }
    $argsCurl = @("-s", "-o", $salida, "-w", "%{http_code}", "-X", $metodo, "-b", $tarro, "-c", $tarro)
    if ($metodo -ne "GET" -and -not $SinCsrf) {
        $argsCurl += @("-H", ("X-XSRF-TOKEN: " + (Xsrf $actor)))
    }
    if ($json -ne "") {
        $cuerpo = Join-Path $TMP "cuerpo.json"
        [System.IO.File]::WriteAllText($cuerpo, $json, $UTF8)
        $argsCurl += @("-H", "Content-Type: application/json", "--data-binary", "@$cuerpo")
    }
    $argsCurl += ($BASE + $ruta)
    $codigo = "$(& curl.exe @argsCurl)".Trim()
    $texto = ""
    if (Test-Path $salida) {
        $texto = [System.IO.File]::ReadAllText($salida, $UTF8)
    }
    return [pscustomobject]@{ Codigo = $codigo; Cuerpo = $texto }
}

function Leer($respuesta) {
    if ($null -eq $respuesta -or $respuesta.Cuerpo -eq "") {
        return $null
    }
    return ($respuesta.Cuerpo | ConvertFrom-Json)
}

function Csrf([string]$actor) {
    return (Api $actor "GET" "/api/sesion/csrf").Codigo
}

function Registrar([string]$actor, [string]$nombre, [string]$email, [string]$codigo) {
    Csrf $actor | Out-Null
    return Api $actor "POST" "/api/cuentas/registro" (ComoJson @{ nombre = $nombre; email = $email; contrasena = $CLAVE; autorizaTratamientoDatos = $true; codigo = $codigo })
}

function EnlaceCurso([long]$idCurso, [string]$email) {
    $idCorreo = Sql "SELECT id_correo_habilitado FROM correo_habilitado WHERE id_curso = $idCurso AND email = '$email' AND fecha_uso IS NULL"
    $r = Api "docente" "POST" "/api/cursos/$idCurso/correos/$idCorreo/enlace"
    return (Leer $r).codigo
}

function Ingresar([string]$actor, [string]$email, [string]$contrasena = $CLAVE) {
    Csrf $actor | Out-Null
    $r = Api $actor "POST" "/api/sesion/login" (ComoJson @{ email = $email; contrasena = $contrasena })
    Csrf $actor | Out-Null
    return $r
}

$ADMIN = "e2e.admin@prueba.invalid"
$CLAVE_ADMIN = "abcdefghijklmnopqrstuvwxyz"
$HASH_ADMIN = '$2a$06$.rCVZVOThsIa97pEDOxvGuRRgzG64bvtJ0938xuqzv18d3ZpQhstC'
$DOCENTE = Correo "docente"
$LIDER = Correo "lider"
$INTEGRANTE = Correo "integrante"

# D0 Nucleo disponible (no cuenta como paso)
if ($Levantar) {
    dc up -d --build nucleo
}
$listo = Esperar { "$(& curl.exe -s -o NUL -w '%{http_code}' "$BASE/actuator/health")".Trim() -eq "200" } 180
if (-not $listo) {
    Write-Host "D0 FALLO: el nucleo no responde en $BASE/actuator/health"
    Remove-Item -Recurse -Force $TMP -ErrorAction SilentlyContinue
    exit 1
}

# D1 Docente registrado por habilitacion (K2, R-10, T-040)
try {
    Sql "INSERT INTO usuario (nombre, email, password_hash, rol_plataforma) SELECT 'E2E Admin', '$ADMIN', '$HASH_ADMIN', 'ADMINISTRADOR' WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE email = '$ADMIN'); UPDATE usuario SET password_hash = '$HASH_ADMIN', fecha_anonimizacion = NULL WHERE email = '$ADMIN'; INSERT INTO correo_habilitado (email, rol_plataforma, id_habilitado_por) SELECT '$DOCENTE', 'DOCENTE', id_usuario FROM usuario WHERE email = '$ADMIN';" | Out-Null
    Ingresar "admin" $ADMIN $CLAVE_ADMIN | Out-Null
    $idCorreoDoc = Sql "SELECT id_correo_habilitado FROM correo_habilitado WHERE email = '$DOCENTE' AND rol_plataforma = 'DOCENTE' AND fecha_uso IS NULL"
    $enlaceDoc = Api "admin" "POST" "/api/admin/docentes/$idCorreoDoc/enlace"
    $r1 = Registrar "docente" "Docente E2E" $DOCENTE (Leer $enlaceDoc).codigo
    $r2 = Ingresar "docente" $DOCENTE
    $r3 = Api "docente" "GET" "/api/sesion/yo"
    $yo = Leer $r3
    if ($r1.Codigo -eq "201" -and $r2.Codigo -eq "200" -and $r3.Codigo -eq "200" -and $yo.rol -eq "DOCENTE") {
        $ok++
        Write-Host "D1 OK"
    } else {
        Write-Host "D1 FALLO: esperado [201, 200, 200, DOCENTE] pero salio [$($r1.Codigo), $($r2.Codigo), $($r3.Codigo), $($yo.rol)]"
    }
} catch {
    Write-Host "D1 FALLO: $($_.Exception.Message)"
}

# D2 Configuracion academica (RF1, RF6, RF14)
try {
    $rp = Api "docente" "POST" "/api/proyectos" (ComoJson @{ nombre = "Proyecto E2E $SUFIJO"; descripcion = "Prueba de dominio"; fechaInicio = (Fecha -10); fechaFin = (Fecha 60); usaMonitores = $false })
    $ID_PROYECTO = (Leer $rp).idProyecto
    $rh = Api "docente" "POST" "/api/proyectos/$ID_PROYECTO/hitos" (ComoJson @{ nombreHito = "Hito E2E"; descripcion = $null; orden = 1; fechaInicio = (Fecha -5); fechaLimite = (Fecha 20); diasInactividadAlerta = 3; notificarDocenteInactividad = $true })
    $ID_HITO = (Leer $rh).idHito
    $rc1 = Api "docente" "POST" "/api/hitos/$ID_HITO/criterios" (ComoJson @{ descripcion = "Diagramas"; pesoPorcentaje = 60; puntajeMaximo = 5 })
    $rc2 = Api "docente" "POST" "/api/hitos/$ID_HITO/criterios" (ComoJson @{ descripcion = "Documento"; pesoPorcentaje = 40; puntajeMaximo = 5 })
    $C1 = (Leer $rc1).idCriterio
    $C2 = (Leer $rc2).idCriterio
    $rcu = Api "docente" "POST" "/api/cursos" (ComoJson @{ nombre = "Curso E2E $SUFIJO"; periodo = "2026-2" })
    $ID_CURSO = (Leer $rcu).idCurso
    $rac = Api "docente" "PUT" "/api/proyectos/$ID_PROYECTO/cursos" (ComoJson @{ idsCursos = @($ID_CURSO); confirmar = $true })
    $rco = Api "docente" "POST" "/api/cursos/$ID_CURSO/correos" (ComoJson @{ emails = @($LIDER, $INTEGRANTE) })
    $re = Api "docente" "POST" "/api/proyectos/$ID_PROYECTO/equipos" (ComoJson @{ nombreEquipo = "Equipo E2E"; idCurso = $ID_CURSO })
    $equipo = Leer $re
    $ID_EQUIPO = $equipo.idEquipo
    $ID_HE = @($equipo.hitos | Where-Object { $_.idHito -eq $ID_HITO })[0].idHitoEquipo
    $codigos = @($rp.Codigo, $rh.Codigo, $rc1.Codigo, $rc2.Codigo, $rcu.Codigo, $rac.Codigo, $rco.Codigo, $re.Codigo) -join ","
    if ($codigos -eq "201,201,201,201,201,200,201,201" -and $ID_HE -gt 0) {
        $ok++
        Write-Host "D2 OK"
    } else {
        Write-Host "D2 FALLO: esperado [201,201,201,201,201,200,201,201, idHitoEquipo > 0] pero salio [$codigos, $ID_HE]"
    }
} catch {
    Write-Host "D2 FALLO: $($_.Exception.Message)"
}

# D3 Estudiantes registrados, equipo con lider y permiso negado (4a, 4b)
try {
    $rl = Registrar "lider" "Lider E2E" $LIDER (EnlaceCurso $ID_CURSO $LIDER)
    $ri = Registrar "integrante" "Integrante E2E" $INTEGRANTE (EnlaceCurso $ID_CURSO $INTEGRANTE)
    $ID_LIDER = (Leer $rl).idUsuario
    $ID_INTEGRANTE = (Leer $ri).idUsuario
    $il = Ingresar "lider" $LIDER
    $ii = Ingresar "integrante" $INTEGRANTE
    $m1 = Api "docente" "POST" "/api/equipos/$ID_EQUIPO/miembros" (ComoJson @{ email = $LIDER })
    $m2 = Api "docente" "POST" "/api/equipos/$ID_EQUIPO/miembros" (ComoJson @{ email = $INTEGRANTE })
    $ml = Api "docente" "PUT" "/api/equipos/$ID_EQUIPO/lider" (ComoJson @{ idUsuario = $ID_LIDER })
    $prohibido = Api "integrante" "POST" "/api/hitos-equipo/$ID_HE/tareas" (ComoJson @{ titulo = "No deberia"; horasEstimadas = 1 })
    $codigos = @($rl.Codigo, $ri.Codigo, $il.Codigo, $ii.Codigo, $m1.Codigo, $m2.Codigo, $ml.Codigo, $prohibido.Codigo) -join ","
    if ($codigos -eq "201,201,200,200,201,201,200,403") {
        $ok++
        Write-Host "D3 OK"
    } else {
        Write-Host "D3 FALLO: esperado [201,201,200,200,201,201,200,403] pero salio [$codigos]"
    }
} catch {
    Write-Host "D3 FALLO: $($_.Exception.Message)"
}

# D4 Tareas, dependencia, ruta critica y bloqueo (RF2, RF3, RF9)
try {
    $t1r = Api "lider" "POST" "/api/hitos-equipo/$ID_HE/tareas" (ComoJson @{ titulo = "Requisitos"; idEncargado = $ID_LIDER; horasEstimadas = 6; esPrerrequisitoCritico = $true })
    $t2r = Api "lider" "POST" "/api/hitos-equipo/$ID_HE/tareas" (ComoJson @{ titulo = "Diagramas"; idEncargado = $ID_INTEGRANTE; horasEstimadas = 8; esPrerrequisitoCritico = $false })
    $T1 = (Leer $t1r).idTarea
    $T2 = (Leer $t2r).idTarea
    $dr = Api "lider" "POST" "/api/tareas/$T2/dependencias" (ComoJson @{ prerrequisitos = @($T1) })
    $rc = Api "lider" "GET" "/api/hitos-equipo/$ID_HE/ruta-critica"
    $criticas = @((Leer $rc).criticas)
    $bloqueada = Api "integrante" "PUT" "/api/tareas/$T2/avance" (ComoJson @{ porcentajeAvance = 50 })
    $codigoBloqueo = (Leer $bloqueada).codigo
    $codigos = @($t1r.Codigo, $t2r.Codigo, $dr.Codigo, $rc.Codigo, $bloqueada.Codigo) -join ","
    $rutaOk = ($criticas -contains $T1) -and ($criticas -contains $T2)
    if ($codigos -eq "201,201,200,200,409" -and $rutaOk -and $codigoBloqueo -eq "TAREA_BLOQUEADA") {
        $ok++
        Write-Host "D4 OK"
    } else {
        Write-Host "D4 FALLO: esperado [201,201,200,200,409, ruta True, TAREA_BLOQUEADA] pero salio [$codigos, ruta $rutaOk, $codigoBloqueo]"
    }
} catch {
    Write-Host "D4 FALLO: $($_.Exception.Message)"
}

# D5 Avance y comentario (RF12, RF13, RF14)
try {
    $a1 = Api "lider" "PUT" "/api/tareas/$T1/avance" (ComoJson @{ porcentajeAvance = 100; observacion = "Listo" })
    $a2 = Api "integrante" "PUT" "/api/tareas/$T2/avance" (ComoJson @{ porcentajeAvance = 100; observacion = "Listo" })
    $co = Api "integrante" "POST" "/api/tareas/$T2/comentarios" (ComoJson @{ contenido = "Diagramas terminados" })
    $codigos = @($a1.Codigo, $a2.Codigo, $co.Codigo) -join ","
    $estados = @((Leer $a1).estado, (Leer $a2).estado) -join ","
    if ($codigos -eq "200,200,201" -and $estados -eq "COMPLETADA,COMPLETADA") {
        $ok++
        Write-Host "D5 OK"
    } else {
        Write-Host "D5 FALLO: esperado [200,200,201, COMPLETADA,COMPLETADA] pero salio [$codigos, $estados]"
    }
} catch {
    Write-Host "D5 FALLO: $($_.Exception.Message)"
}

# D6 Envio del hito: estado y evento en la outbox (RF7)
try {
    $en = Api "integrante" "POST" "/api/hitos-equipo/$ID_HE/envio"
    $estado = Sql "select estado from hito_equipo where id_hito_equipo = $ID_HE"
    $eventos = Sql "select count(*) from evento_saliente where tipo = 'hito.enviado' and (carga->'data'->'hito'->>'id')::bigint = $ID_HE"
    if ($en.Codigo -eq "201" -and $estado -eq "ENVIADO" -and $eventos -eq "1") {
        $ok++
        Write-Host "D6 OK"
    } else {
        Write-Host "D6 FALLO: esperado [201, ENVIADO, 1] pero salio [$($en.Codigo), $estado, $eventos]"
    }
} catch {
    Write-Host "D6 FALLO: $($_.Exception.Message)"
}

# D7 Evaluacion: nota grupal de la rubrica, vista del estudiante y confirmacion (RF8, C1, E1)
try {
    $ev = Api "docente" "POST" "/api/hitos-equipo/$ID_HE/evaluacion" (ComoJson @{ criterios = @(@{ idCriterio = $C1; nota = 4 }, @{ idCriterio = $C2; nota = 3 }); retroalimentacion = "Buen trabajo" })
    $evaluacion = Leer $ev
    $ID_EVALUACION = $evaluacion.idEvaluacion
    $vista = Api "integrante" "GET" "/api/hitos-equipo/$ID_HE/evaluacion"
    $propia = Leer $vista
    $notas = @($propia.notas)
    $cf = Api "docente" "PUT" "/api/evaluaciones/$ID_EVALUACION/notas/$ID_INTEGRANTE" '{"notaFinal":3.5}'
    $codigos = @($ev.Codigo, $vista.Codigo, $cf.Codigo) -join ","
    $grupalOk = ([double]$evaluacion.notaGrupal) -eq 3.6
    $soloSuya = ($notas.Count -eq 1) -and ($notas[0].idEstudiante -eq $ID_INTEGRANTE) -and ($null -eq $propia.notaCalculada)
    if ($codigos -eq "201,200,200" -and $grupalOk -and $soloSuya) {
        $ok++
        Write-Host "D7 OK"
    } else {
        Write-Host "D7 FALLO: esperado [201,200,200, grupal 3.6, solo su fila True] pero salio [$codigos, grupal $($evaluacion.notaGrupal), solo su fila $soloSuya]"
    }
} catch {
    Write-Host "D7 FALLO: $($_.Exception.Message)"
}

# D8 Bandeja interna (N1, P10)
try {
    $bd = Api "docente" "GET" "/api/notificaciones"
    $enviado = @((Leer $bd).notificaciones | Where-Object { $_.tipo -eq "hito.enviado" -and $_.idReferencia -eq $ID_HE }).Count
    $bi = Api "integrante" "GET" "/api/notificaciones"
    $evaluado = @((Leer $bi).notificaciones | Where-Object { $_.tipo -eq "hito.evaluado" -and $_.idReferencia -eq $ID_HE }).Count
    $todas = Api "docente" "POST" "/api/notificaciones/leidas"
    $despues = (Leer (Api "docente" "GET" "/api/notificaciones")).noLeidas
    $codigos = @($bd.Codigo, $bi.Codigo, $todas.Codigo) -join ","
    if ($codigos -eq "200,200,204" -and $enviado -eq 1 -and $evaluado -eq 1 -and $despues -eq 0) {
        $ok++
        Write-Host "D8 OK"
    } else {
        Write-Host "D8 FALLO: esperado [200,200,204, 1, 1, 0] pero salio [$codigos, $enviado, $evaluado, $despues]"
    }
} catch {
    Write-Host "D8 FALLO: $($_.Exception.Message)"
}

# D9 Reporte PDF, CSRF obligatorio y cierre de sesion (RF15, R-12)
try {
    $pdf = Api "docente" "GET" "/api/equipos/$ID_EQUIPO/reporte.pdf"
    $esPdf = $pdf.Cuerpo.StartsWith("%PDF-")
    $sinCsrf = Api "docente" "POST" "/api/notificaciones/leidas" -SinCsrf
    $codigoCsrf = (Leer $sinCsrf).codigo
    $salir = Api "integrante" "POST" "/api/sesion/logout"
    $yo = Api "integrante" "GET" "/api/sesion/yo"
    $codigos = @($pdf.Codigo, $sinCsrf.Codigo, $salir.Codigo, $yo.Codigo) -join ","
    if ($codigos -eq "200,403,204,401" -and $esPdf -and $codigoCsrf -eq "CSRF_INVALIDO") {
        $ok++
        Write-Host "D9 OK"
    } else {
        Write-Host "D9 FALLO: esperado [200,403,204,401, pdf True, CSRF_INVALIDO] pero salio [$codigos, pdf $esPdf, $codigoCsrf]"
    }
} catch {
    Write-Host "D9 FALLO: $($_.Exception.Message)"
}

# D10 reservado: supresion de la cuenta (bloque 27, se agrega con el 26f)
# D11 Contrasena: recuperacion sin enumeracion y cambio con sesion (bloque 28). Sin Telegram: el lider no tiene chat.
try {
    Sql "DELETE FROM limite_recuperacion;" | Out-Null
    $NUEVA = "Nueva-$CLAVE"
    Csrf "anonimo" | Out-Null
    $s1 = Api "anonimo" "POST" "/api/cuentas/recuperacion" (ComoJson @{ email = $LIDER })
    $s2 = Api "anonimo" "POST" "/api/cuentas/recuperacion" (ComoJson @{ email = (Correo "nadie") })
    $rb = Api "anonimo" "POST" "/api/cuentas/recuperacion/restablecer" (ComoJson @{ codigo = ("A" * 43); contrasenaNueva = $NUEVA })
    $codigoRb = (Leer $rb).codigo
    Ingresar "lider2" $LIDER | Out-Null
    $cm = Api "lider" "POST" "/api/perfil/contrasena" (ComoJson @{ contrasenaActual = $CLAVE; contrasenaNueva = $NUEVA })
    $yoLider = Api "lider" "GET" "/api/sesion/yo"
    $yoOtra = Api "lider2" "GET" "/api/sesion/yo"
    Csrf "nuevo" | Out-Null
    $vieja = Api "nuevo" "POST" "/api/sesion/login" (ComoJson @{ email = $LIDER; contrasena = $CLAVE })
    $nueva = Api "nuevo" "POST" "/api/sesion/login" (ComoJson @{ email = $LIDER; contrasena = $NUEVA })
    $codigos = @($s1.Codigo, $s2.Codigo, $rb.Codigo, $cm.Codigo, $yoLider.Codigo, $yoOtra.Codigo, $vieja.Codigo, $nueva.Codigo) -join ","
    $iguales = ($s1.Cuerpo -eq $s2.Cuerpo)
    if ($codigos -eq "202,202,400,204,200,401,401,200" -and $iguales -and $codigoRb -eq "CODIGO_INVALIDO") {
        $ok++
        Write-Host "D11 OK"
    } else {
        Write-Host "D11 FALLO: esperado [202,202,400,204,200,401,401,200, iguales True, CODIGO_INVALIDO] pero salio [$codigos, iguales $iguales, $codigoRb]"
    }
} catch {
    Write-Host "D11 FALLO: $($_.Exception.Message)"
}

Remove-Item -Recurse -Force $TMP -ErrorAction SilentlyContinue
Write-Host "RESULTADO: $ok/$TOTAL pasos OK"

if ($ok -eq $TOTAL) {
    exit 0
} else {
    exit 1
}
```

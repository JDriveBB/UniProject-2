param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Levantar', 'Bajar', 'Reiniciar', 'Estado', 'LimpiarLimites', 'RepetirAlertas')]
    [string]$Accion,

    [switch]$BorrarDatos,
    [switch]$SinConfirmar
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$PROYECTO = 'uniproject-pruebas'
$PROYECTO_DEV = 'uniproject'
$URL = 'http://127.0.0.1:18080'
$PUERTO_WEB = 18080
$PUERTO_BD = 25432
$PUERTO_AMQP = 25672
$PUERTO_PANEL_RABBITMQ = 25673
$ETIQUETA_IMAGEN = 'pruebas'
$DOMINIO = 'demo.uniproject.test'
$ADMIN_EMAIL = "admin@$DOMINIO"
$DIR_CRED = Join-Path $env:LOCALAPPDATA 'UniProject\sesion-pruebas'
$ARCHIVO_CRED = Join-Path $DIR_CRED 'credenciales.txt'

function dc {
    $input | docker compose -p $PROYECTO --env-file .env -f infra/docker-compose.yml @args
}

function EstadoVariableEnv([string]$nombre) {
    if (-not (Test-Path '.env')) {
        throw 'falta .env en la raiz'
    }
    $encontrado = $false
    $tieneValor = $false
    foreach ($linea in [System.IO.File]::ReadLines((Resolve-Path '.env').Path)) {
        $trimmed = $linea.Trim()
        if ($trimmed.StartsWith('#')) { continue }
        $idx = $trimmed.IndexOf('=')
        if ($idx -gt 0) {
            if ($trimmed.Substring(0, $idx).Trim() -eq $nombre) {
                $encontrado = $true
                if ($trimmed.Substring($idx + 1).Trim().Trim('"').Trim("'").Trim().Length -gt 0) {
                    $tieneValor = $true
                }
                break
            }
        }
    }
    if (-not $encontrado) { return 'ausente' }
    if ($tieneValor) { return 'no vacio' }
    return 'vacio'
}

function NuevaClave([string]$prefijo) {
    $bytes = New-Object byte[] 10
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $hex = ($bytes | ForEach-Object { $_.ToString('x2') }) -join ''
    return ($prefijo + $hex)
}

function LeerCredenciales {
    if (-not (Test-Path $ARCHIVO_CRED)) {
        return $null
    }
    $demo = $null
    $admin = $null
    foreach ($linea in [System.IO.File]::ReadAllLines($ARCHIVO_CRED)) {
        if ($linea.StartsWith('DEMO_CONTRASENA=')) {
            $demo = $linea.Substring('DEMO_CONTRASENA='.Length).Trim()
        } elseif ($linea.StartsWith('ADMIN_CONTRASENA=')) {
            $admin = $linea.Substring('ADMIN_CONTRASENA='.Length).Trim()
        }
    }
    if ([string]::IsNullOrEmpty($demo) -or [string]::IsNullOrEmpty($admin)) {
        throw 'credenciales.txt incompleto: usa -Accion Reiniciar'
    }
    return [pscustomobject]@{
        Demo  = $demo
        Admin = $admin
    }
}

function EscribirCredenciales([string]$demo, [string]$admin) {
    if (-not (Test-Path $DIR_CRED)) {
        [System.IO.Directory]::CreateDirectory($DIR_CRED) | Out-Null
    }
    $fecha = (Get-Date).ToString('yyyy-MM-dd HH:mm')
    $lineas = @(
        '# UniProject - sesion de pruebas. NO subir a git ni compartir fuera del equipo.',
        "DEMO_CONTRASENA=$demo",
        "ADMIN_CONTRASENA=$admin",
        "URL=$URL",
        "Generado=$fecha",
        '',
        'Cuentas:',
        "docente@$DOMINIO Laura Mendez DOCENTE",
        "ana@$DOMINIO Ana Rojas lider Equipo Alfa",
        "bruno@$DOMINIO Bruno Diaz Equipo Alfa",
        "camila@$DOMINIO Camila Torres Equipo Alfa",
        "diego@$DOMINIO Diego Perez lider Equipo Beta",
        "elena@$DOMINIO Elena Gomez Equipo Beta",
        "felipe@$DOMINIO Felipe Ruiz Equipo Beta",
        "monitor@$DOMINIO Gabriela Soto MONITOR",
        "hugo@$DOMINIO Hugo Lara estudiante sin equipo (sacrificio)",
        "$ADMIN_EMAIL ADMINISTRADOR (usa ADMIN_CONTRASENA)",
        '',
        "Correos habilitados sin cuenta: ines@$DOMINIO, jorge@$DOMINIO"
    )
    $utf8SinBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($ARCHIVO_CRED, $lineas, $utf8SinBom)
}

function ConVariables([scriptblock]$bloque) {
    $demoVal = ''
    $adminVal = ''
    if ($Accion -ne 'Bajar') {
        try {
            $c = LeerCredenciales
            if ($null -ne $c) {
                $demoVal = $c.Demo
                $adminVal = $c.Admin
            }
        } catch {
        }
    }

    $nombres = @(
        'UNIPROJECT_PUBLICAR_WEB',
        'UNIPROJECT_PUERTO_BD',
        'UNIPROJECT_PUERTO_AMQP',
        'UNIPROJECT_PUERTO_PANEL_RABBITMQ',
        'UNIPROJECT_ETIQUETA_IMAGEN',
        'UNIPROJECT_PERFILES',
        'UNIPROJECT_DEMO_CONTRASENA',
        'UNIPROJECT_ADMIN_EMAIL',
        'UNIPROJECT_ADMIN_NOMBRE',
        'UNIPROJECT_ADMIN_CONTRASENA',
        'UNIPROJECT_URL_BASE',
        'UNIPROJECT_TELEGRAM_BOTON_ENLACE',
        'UNIPROJECT_PROGRAMADOR_CRONRIESGO',
        'UNIPROJECT_PROGRAMADOR_CRONINACTIVIDAD'
    )

    $nuevas = [ordered]@{
        'UNIPROJECT_PUBLICAR_WEB'               = "127.0.0.1:$PUERTO_WEB"
        'UNIPROJECT_PUERTO_BD'                   = "$PUERTO_BD"
        'UNIPROJECT_PUERTO_AMQP'                 = "$PUERTO_AMQP"
        'UNIPROJECT_PUERTO_PANEL_RABBITMQ'       = "$PUERTO_PANEL_RABBITMQ"
        'UNIPROJECT_ETIQUETA_IMAGEN'             = $ETIQUETA_IMAGEN
        'UNIPROJECT_PERFILES'                    = 'demo'
        'UNIPROJECT_DEMO_CONTRASENA'             = $demoVal
        'UNIPROJECT_ADMIN_EMAIL'                 = $ADMIN_EMAIL
        'UNIPROJECT_ADMIN_NOMBRE'                = 'Administrador (pruebas)'
        'UNIPROJECT_ADMIN_CONTRASENA'            = $adminVal
        'UNIPROJECT_URL_BASE'                    = $URL
        'UNIPROJECT_TELEGRAM_BOTON_ENLACE'       = 'false'
        'UNIPROJECT_PROGRAMADOR_CRONRIESGO'      = '0 */5 * * * *'
        'UNIPROJECT_PROGRAMADOR_CRONINACTIVIDAD' = '0 */5 * * * *'
    }

    $previas = @{}
    foreach ($k in $nombres) {
        $item = Get-Item -Path "Env:$k" -ErrorAction SilentlyContinue
        if ($null -ne $item) {
            $previas[$k] = $item.Value
        } else {
            $previas[$k] = $null
        }
        [Environment]::SetEnvironmentVariable($k, $nuevas[$k], 'Process')
    }

    try {
        & $bloque
    } finally {
        foreach ($k in $nombres) {
            if ($null -eq $previas[$k]) {
                [Environment]::SetEnvironmentVariable($k, $null, 'Process')
            } else {
                [Environment]::SetEnvironmentVariable($k, $previas[$k], 'Process')
            }
        }
    }
}

function HuellaDesarrollo {
    $ps = docker ps -a --filter "label=com.docker.compose.project=$PROYECTO_DEV" --format '{{.ID}} {{.Names}} {{.State}}'
    $vols = docker volume ls -q --filter "label=com.docker.compose.project=$PROYECTO_DEV"
    $todos = @()
    if ($null -ne $ps) { $todos += $ps }
    if ($null -ne $vols) { $todos += $vols }
    return ($todos | Sort-Object | Out-String).Trim()
}

function ExigirSinCambiosEnDesarrollo($antes) {
    $despues = HuellaDesarrollo
    if ($antes -ne $despues) {
        Write-Host 'ALERTA: cambio el stack de desarrollo'
        exit 1
    }
}

function Sql([string]$consulta) {
    $salida = $consulta | dc exec -T base-de-datos psql -U postgres -d uniproject -At -v ON_ERROR_STOP=1
    if ($null -eq $salida) {
        return ''
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

function PuertoLibre([int]$p) {
    $ip1 = [System.Net.IPAddress]::Loopback
    $ip2 = [System.Net.IPAddress]::Any
    $l1 = $null
    $l2 = $null
    try {
        $l1 = New-Object System.Net.Sockets.TcpListener($ip1, $p)
        $l1.Start()
    } catch {
        return $false
    } finally {
        if ($null -ne $l1) {
            try { $l1.Stop() } catch {}
        }
    }
    try {
        $l2 = New-Object System.Net.Sockets.TcpListener($ip2, $p)
        $l2.Start()
    } catch {
        return $false
    } finally {
        if ($null -ne $l2) {
            try { $l2.Stop() } catch {}
        }
    }
    return $true
}

function ExigirStackPruebasCorriendo {
    $serviciosPruebas = ''
    try {
        $serviciosPruebas = (dc ps --status running -q 2>$null) -join ''
    } catch {}
    if ([string]::IsNullOrWhiteSpace($serviciosPruebas)) {
        Write-Host 'Error: el stack de pruebas no esta corriendo'
        exit 1
    }
}

function EjecutarLevantar {
    $antes = HuellaDesarrollo
    $estadoToken = EstadoVariableEnv 'TELEGRAM_BOT_TOKEN'
    $estadoUser = EstadoVariableEnv 'TELEGRAM_BOT_USERNAME'
    Write-Host "TELEGRAM_BOT_TOKEN: $estadoToken"
    Write-Host "TELEGRAM_BOT_USERNAME: $estadoUser"

    if ($estadoToken -eq 'vacio' -or $estadoToken -eq 'ausente') {
        Write-Host 'Telegram no se podra probar (seccion Notificaciones de la guia)'
    } else {
        $serviciosDev = ''
        try {
            $serviciosDev = (docker compose -p $PROYECTO_DEV ps --status running --services 2>$null) -join ' '
        } catch {}
        if ($serviciosDev -match '\bagente\b') {
            $logsDev = ''
            try {
                $logsDev = (docker compose -p $PROYECTO_DEV logs agente 2>$null) -join "`n"
            } catch {}
            if ($logsDev -notmatch 'TELEGRAM_BOT_TOKEN vac') {
                Write-Host 'El agente de desarrollo usa el mismo bot. Detenlo con: docker compose -p uniproject stop agente'
                exit 1
            }
        }
    }

    $serviciosPruebas = ''
    try {
        $serviciosPruebas = (docker compose -p $PROYECTO ps --status running --services 2>$null) -join ' '
    } catch {}
    if ([string]::IsNullOrWhiteSpace($serviciosPruebas)) {
        foreach ($p in @($PUERTO_WEB, $PUERTO_BD, $PUERTO_AMQP, $PUERTO_PANEL_RABBITMQ)) {
            if (-not (PuertoLibre $p)) {
                Write-Host "El puerto $p esta ocupado"
                exit 1
            }
        }
    }

    $volExiste = $false
    try {
        $vols = (docker volume ls -q --filter "name=${PROYECTO}_datos-postgres" 2>$null) -join "`n"
        if ($vols -match "${PROYECTO}_datos-postgres") {
            $volExiste = $true
        }
    } catch {}

    $cred = LeerCredenciales
    if ($null -eq $cred -and $volExiste) {
        Write-Host 'Hay datos de pruebas sin credenciales: usa -Accion Reiniciar'
        exit 1
    }
    if ($null -eq $cred -and -not $volExiste) {
        $cDemo = NuevaClave 'Demo'
        $cAdmin = NuevaClave 'Admin'
        EscribirCredenciales $cDemo $cAdmin
        $cred = LeerCredenciales
    }
    [Environment]::SetEnvironmentVariable('UNIPROJECT_DEMO_CONTRASENA', $cred.Demo, 'Process')
    [Environment]::SetEnvironmentVariable('UNIPROJECT_ADMIN_CONTRASENA', $cred.Admin, 'Process')

    dc up -d --build
    if ($LASTEXITCODE -ne 0) {
        # el primer arranque de RabbitMQ con volumen vacio falla una vez (cookie de Erlang) y se recupera solo; un reintento basta
        Write-Host 'Primer intento fallido; reintentando una vez...'
        Start-Sleep -Seconds 20
        dc up -d
    }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error al levantar contenedores (exit code $LASTEXITCODE)"
        exit 1
    }

    $script:fallaInmediata = $null
    $script:condicionFalto = ''
    $script:servicioFallo = 'nucleo'

    $listo = Esperar {
        $logsNucleo = ''
        try { $logsNucleo = (dc logs nucleo 2>$null) -join "`n" } catch {}
        if ($logsNucleo -match 'Datos demo NO cargados') {
            $script:fallaInmediata = 'Datos demo NO cargados'
            $script:servicioFallo = 'nucleo'
            return $true
        }
        if ($logsNucleo -match 'UNIPROJECT_DEMO_CONTRASENA est') {
            $script:fallaInmediata = 'UNIPROJECT_DEMO_CONTRASENA esta vacia'
            $script:servicioFallo = 'nucleo'
            return $true
        }

        $psBd = ''
        try { $psBd = (dc ps --format '{{.Health}}' base-de-datos 2>$null) -join '' } catch {}
        if ($psBd -notmatch 'healthy') {
            $script:condicionFalto = 'base-de-datos healthy'
            $script:servicioFallo = 'base-de-datos'
            return $false
        }

        $psRabbit = ''
        try { $psRabbit = (dc ps --format '{{.Health}}' rabbitmq 2>$null) -join '' } catch {}
        if ($psRabbit -notmatch 'healthy') {
            $script:condicionFalto = 'rabbitmq healthy'
            $script:servicioFallo = 'rabbitmq'
            return $false
        }

        $psInit = ''
        try {
            $psInit = (docker ps -a --filter "label=com.docker.compose.project=$PROYECTO" --filter "label=com.docker.compose.service=rabbitmq-inicializador" --format '{{.Status}}' 2>$null) -join ''
        } catch {}
        if ($psInit -notmatch 'Exited \(0\)') {
            $script:condicionFalto = 'rabbitmq-inicializador terminado con 0'
            $script:servicioFallo = 'rabbitmq-inicializador'
            return $false
        }

        $healthCode = ''
        try {
            $healthCode = "$(& curl.exe -s -o NUL -w '%{http_code}' "$URL/actuator/health")".Trim()
        } catch {}
        if ($healthCode -ne '200') {
            $script:condicionFalto = "GET $URL/actuator/health = 200 (obtenido: $healthCode)"
            $script:servicioFallo = 'nucleo'
            return $false
        }

        if ($logsNucleo -notmatch 'Datos demo cargados' -and $logsNucleo -notmatch 'Los datos demo ya exist') {
            $script:condicionFalto = 'log nucleo: Datos demo cargados o Los datos demo ya existian'
            $script:servicioFallo = 'nucleo'
            return $false
        }

        $logsAgente = ''
        try { $logsAgente = (dc logs agente 2>$null) -join "`n" } catch {}
        if ($logsAgente -notmatch 'Started AgenteAplicacion') {
            $script:condicionFalto = 'log agente: Started AgenteAplicacion'
            $script:servicioFallo = 'agente'
            return $false
        }

        return $true
    } 300

    if ($null -ne $script:fallaInmediata) {
        Write-Host "Falla inmediata: $script:fallaInmediata"
        dc logs --tail 30 nucleo
        exit 1
    }

    if (-not $listo) {
        Write-Host "Fallo de espera (300 s). Condicion que falto: $script:condicionFalto"
        Write-Host "Ultimas 30 lineas de log de $script:servicioFallo`:"
        dc logs --tail 30 $script:servicioFallo
        exit 1
    }

    $logsNucleoActual = ''
    try { $logsNucleoActual = (dc logs nucleo 2>$null) -join "`n" } catch {}
    if ($logsNucleoActual -match 'Datos demo cargados') {
        $cuentas = Sql "SELECT count(*) FROM usuario WHERE email LIKE '%@$DOMINIO' AND rol_plataforma != 'ADMINISTRADOR';"
        if ($cuentas -eq '8') {
            Write-Host 'escenario antiguo: usa -Accion Reiniciar'
            exit 1
        }
        if ($cuentas -ne '9') {
            Write-Host "Error: se esperaban 9 cuentas demo no administrador, pero hay $cuentas"
            exit 1
        }
    }
    $admin = Sql "SELECT count(*) FROM usuario WHERE rol_plataforma = 'ADMINISTRADOR';"
    if ([int]$admin -lt 1) {
        Write-Host 'Error: no hay usuario ADMINISTRADOR'
        exit 1
    }
    if ($estadoToken -ne 'vacio' -and $estadoToken -ne 'ausente') {
        $logsAgente = (dc logs agente 2>$null) -join "`n"
        if ($logsAgente -match 'TELEGRAM_BOT_TOKEN vac') {
            Write-Host 'Error: agente reporta TELEGRAM_BOT_TOKEN vacio'
            exit 1
        }
    }

    ExigirSinCambiosEnDesarrollo $antes

    Write-Host "URL: $URL"
    Write-Host "Credenciales: $ARCHIVO_CRED"
    Write-Host 'Cuentas:'
    Write-Host "  docente@$DOMINIO Laura Mendez DOCENTE"
    Write-Host "  ana@$DOMINIO Ana Rojas lider Equipo Alfa"
    Write-Host "  bruno@$DOMINIO Bruno Diaz Equipo Alfa"
    Write-Host "  camila@$DOMINIO Camila Torres Equipo Alfa"
    Write-Host "  diego@$DOMINIO Diego Perez lider Equipo Beta"
    Write-Host "  elena@$DOMINIO Elena Gomez Equipo Beta"
    Write-Host "  felipe@$DOMINIO Felipe Ruiz Equipo Beta"
    Write-Host "  monitor@$DOMINIO Gabriela Soto MONITOR"
    Write-Host "  hugo@$DOMINIO Hugo Lara estudiante sin equipo (sacrificio)"
    Write-Host "  $ADMIN_EMAIL ADMINISTRADOR (usa ADMIN_CONTRASENA)"
    Write-Host "  Correos habilitados sin cuenta: ines@$DOMINIO, jorge@$DOMINIO"
    Write-Host "Panel RabbitMQ: http://127.0.0.1:$PUERTO_PANEL_RABBITMQ (usuario y contrasena de administrador de .env)"
    Write-Host "Estado Telegram: token=$estadoToken, username=$estadoUser"
    Write-Host 'Alertas programadas cada 5 min'
}

function EjecutarEstado {
    Write-Host '--- Servicios de pruebas ---'
    dc ps --format 'table {{.Service}}\t{{.State}}\t{{.Health}}'

    $healthCode = ''
    try {
        $healthCode = "$(& curl.exe -s -o NUL -w '%{http_code}' "$URL/actuator/health")".Trim()
    } catch {}
    Write-Host "Codigo HTTP /actuator/health: $healthCode"

    $logsNucleo = ''
    try { $logsNucleo = (dc logs nucleo 2>$null) -join "`n" } catch {}
    if ($logsNucleo -match 'Datos demo cargados') {
        Write-Host 'Resultado carga demo: Datos demo cargados'
    } elseif ($logsNucleo -match 'Los datos demo ya exist') {
        Write-Host 'Resultado carga demo: Los datos demo ya existian'
    } elseif ($logsNucleo -match 'Datos demo NO cargados') {
        Write-Host 'Resultado carga demo: Datos demo NO cargados'
    } else {
        Write-Host 'Resultado carga demo: No registrado en logs recientes'
    }

    $estadoToken = EstadoVariableEnv 'TELEGRAM_BOT_TOKEN'
    $estadoUser = EstadoVariableEnv 'TELEGRAM_BOT_USERNAME'
    Write-Host "TELEGRAM_BOT_TOKEN: $estadoToken"
    Write-Host "TELEGRAM_BOT_USERNAME: $estadoUser"

    if (Test-Path $ARCHIVO_CRED) {
        Write-Host "Archivo credenciales: existe ($ARCHIVO_CRED)"
    } else {
        Write-Host "Archivo credenciales: no existe ($ARCHIVO_CRED)"
    }

    $serviciosDev = ''
    try {
        $serviciosDev = (docker compose -p $PROYECTO_DEV ps --status running --services 2>$null) -join ' '
    } catch {}
    if ($serviciosDev -match '\bagente\b') {
        Write-Host 'Agente de desarrollo: corriendo'
    } else {
        Write-Host 'Agente de desarrollo: no corriendo'
    }
}

function EjecutarBajar([switch]$Borrar, [switch]$SinConf) {
    $antes = HuellaDesarrollo
    if ($Borrar) {
        if (-not $SinConf) {
            $resp = Read-Host 'Esta seguro de borrar los datos de pruebas? Escriba SI para confirmar'
            if ($resp -ne 'SI') {
                Write-Host 'Operacion cancelada'
                return
            }
        }
        dc down -v --remove-orphans
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Error al bajar contenedores con volumenes (exit code $LASTEXITCODE)"
            exit 1
        }
        if (Test-Path $ARCHIVO_CRED) {
            Remove-Item -Force $ARCHIVO_CRED -ErrorAction SilentlyContinue
            Write-Host "Archivo de credenciales borrado ($ARCHIVO_CRED)"
        }
    } else {
        dc down --remove-orphans
    }
    ExigirSinCambiosEnDesarrollo $antes
}

function EjecutarReiniciar([switch]$SinConf) {
    if (-not $SinConf) {
        $resp = Read-Host 'Esta seguro de reiniciar la sesion de pruebas? Escriba SI para confirmar'
        if ($resp -ne 'SI') {
            Write-Host 'Operacion cancelada'
            return
        }
    }
    EjecutarBajar -Borrar -SinConf
    EjecutarLevantar
}

function EjecutarLimpiarLimites {
    ExigirStackPruebasCorriendo
    $borradas = Sql 'WITH d AS (DELETE FROM limite_recuperacion RETURNING 1) SELECT count(*) FROM d;'
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error al limpiar tabla limite_recuperacion (exit code $LASTEXITCODE)"
        exit 1
    }
    Write-Host "Filas borradas en limite_recuperacion: $borradas"
}

function EjecutarRepetirAlertas {
    ExigirStackPruebasCorriendo
    $b1 = Sql 'WITH d AS (DELETE FROM estado_riesgo_tarea RETURNING 1) SELECT count(*) FROM d;'
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error al limpiar tabla estado_riesgo_tarea (exit code $LASTEXITCODE)"
        exit 1
    }
    $b2 = Sql 'WITH d AS (DELETE FROM alerta_inactividad RETURNING 1) SELECT count(*) FROM d;'
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error al limpiar tabla alerta_inactividad (exit code $LASTEXITCODE)"
        exit 1
    }
    Write-Host "Filas borradas: estado_riesgo_tarea: $b1, alerta_inactividad: $b2"
    Write-Host 'La proxima pasada (minuto multiplo de 5) vuelve a avisar'
}

ConVariables {
    switch ($Accion) {
        'Levantar'       { EjecutarLevantar }
        'Bajar'          { EjecutarBajar -Borrar:$BorrarDatos -SinConf:$SinConfirmar }
        'Reiniciar'      { EjecutarReiniciar -SinConf:$SinConfirmar }
        'Estado'         { EjecutarEstado }
        'LimpiarLimites' { EjecutarLimpiarLimites }
        'RepetirAlertas' { EjecutarRepetirAlertas }
    }
}

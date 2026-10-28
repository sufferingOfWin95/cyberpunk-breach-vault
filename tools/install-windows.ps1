# Устанавливает Breach Vault для текущего пользователя (права администратора не нужны):
#   1) копирует build\jpackage\out\BreachVault в %LOCALAPPDATA%\BreachVault  (вне OneDrive);
#   2) кладёт ярлык «Breach Vault» на рабочий стол;
#   3) связывает файлы .vault с программой — двойной клик открывает дешифратор.
# Перед запуском собери программу:  .\gradlew.bat packageApp
# Запуск:  powershell -ExecutionPolicy Bypass -File tools\install-windows.ps1
# Удаление: tools\uninstall-windows.ps1
$ErrorActionPreference = 'Stop'

$source = Join-Path (Split-Path $PSScriptRoot) 'build\jpackage\out\BreachVault'
$target = Join-Path $env:LOCALAPPDATA 'BreachVault'
$exe = Join-Path $target 'BreachVault.exe'

if (-not (Test-Path (Join-Path $source 'BreachVault.exe'))) {
    throw "Не найдена собранная программа в $source. Сначала: .\gradlew.bat packageApp"
}

# Старую установку заменяем, но только если это точно наша папка
if (Test-Path $target) {
    if (-not (Test-Path $exe)) { throw "$target существует, но это не Breach Vault. Остановка." }
    Get-Process BreachVault -ErrorAction SilentlyContinue | Stop-Process -Force
    Remove-Item -Recurse -Force $target
}
Copy-Item -Recurse $source $target
"Программа:  $target"

# Ярлык на рабочем столе
$desktop = [Environment]::GetFolderPath('Desktop')
$shell = New-Object -ComObject WScript.Shell
$link = $shell.CreateShortcut((Join-Path $desktop 'Breach Vault.lnk'))
$link.TargetPath = $exe
$link.WorkingDirectory = $target
$link.IconLocation = "$exe,0"
$link.Save()
"Ярлык:      $(Join-Path $desktop 'Breach Vault.lnk')"

# Связь .vault -> BreachVault.exe (только для текущего пользователя, ветка HKCU)
$classes = 'HKCU:\Software\Classes'
$ext = Join-Path $classes '.vault'
if (Test-Path $ext) {
    $current = (Get-Item $ext).GetValue('')
    if ($current -and $current -ne 'BreachVault.Vault') {
        throw ".vault уже связан с '$current'. Не трогаю чужую настройку."
    }
}
New-Item -Path $ext -Value 'BreachVault.Vault' -Force | Out-Null
New-Item -Path "$classes\BreachVault.Vault" -Value 'Breach Vault' -Force | Out-Null
New-Item -Path "$classes\BreachVault.Vault\DefaultIcon" -Value "`"$exe`",0" -Force | Out-Null
New-Item -Path "$classes\BreachVault.Vault\shell\open\command" -Value "`"$exe`" `"%1`"" -Force | Out-Null

# Сообщаем проводнику, что связи файлов изменились, — чтобы иконки обновились сразу
Add-Type -Namespace Win32 -Name Shell -MemberDefinition '[DllImport("shell32.dll")] public static extern void SHChangeNotify(int e, int f, System.IntPtr a, System.IntPtr b);'
[Win32.Shell]::SHChangeNotify(0x08000000, 0, [IntPtr]::Zero, [IntPtr]::Zero)
"Связь:      *.vault -> $exe"

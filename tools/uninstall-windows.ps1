# Отменяет всё, что сделал install-windows.ps1: связь .vault, ярлык и папку программы.
# Сами файлы .vault и расшифрованные папки НЕ трогает.
# Запуск:  powershell -ExecutionPolicy Bypass -File tools\uninstall-windows.ps1
$ErrorActionPreference = 'Stop'

$target = Join-Path $env:LOCALAPPDATA 'BreachVault'
$classes = 'HKCU:\Software\Classes'

# Связь .vault снимаем, только если она наша
$ext = Join-Path $classes '.vault'
if ((Test-Path $ext) -and (Get-Item $ext).GetValue('') -eq 'BreachVault.Vault') {
    Remove-Item -Recurse $ext
}
if (Test-Path "$classes\BreachVault.Vault") {
    Remove-Item -Recurse "$classes\BreachVault.Vault"
}

$link = Join-Path ([Environment]::GetFolderPath('Desktop')) 'Breach Vault.lnk'
if (Test-Path $link) { Remove-Item $link }

if (Test-Path (Join-Path $target 'BreachVault.exe')) {
    Get-Process BreachVault -ErrorAction SilentlyContinue | Stop-Process -Force
    Remove-Item -Recurse -Force $target
}

Add-Type -Namespace Win32 -Name Shell -MemberDefinition '[DllImport("shell32.dll")] public static extern void SHChangeNotify(int e, int f, System.IntPtr a, System.IntPtr b);'
[Win32.Shell]::SHChangeNotify(0x08000000, 0, [IntPtr]::Zero, [IntPtr]::Zero)
"Breach Vault удалён."

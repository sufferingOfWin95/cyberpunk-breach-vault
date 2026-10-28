# Рисует иконку Breach Vault в нескольких размерах:
#   src/main/resources/breachvault/icons/icon-N.png  — для окна и панели задач (JavaFX)
#   src/main/packaging/breachvault.ico                — для .exe и файлов .vault (Windows)
# Запуск: powershell -ExecutionPolicy Bypass -File tools\make-icon.ps1
Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'

$root = Split-Path $PSScriptRoot
$pngDir = Join-Path $root 'src\main\resources\breachvault\icons'
$icoPath = Join-Path $root 'src\main\packaging\breachvault.ico'
New-Item -ItemType Directory -Force $pngDir, (Split-Path $icoPath) | Out-Null

$bg = [System.Drawing.ColorTranslator]::FromHtml('#0D0F14')
$lime = [System.Drawing.ColorTranslator]::FromHtml('#D0ED57')

function Draw-Icon([int]$size) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.Clear([System.Drawing.Color]::Transparent)
    $s = $size / 64.0   # рисуем в координатах 64×64 и масштабируем

    # Фон и рамка
    $g.FillRectangle((New-Object System.Drawing.SolidBrush $bg), 0, 0, $size, $size)
    $pen = New-Object System.Drawing.Pen $lime, ([Math]::Max(1, 2 * $s))
    $g.DrawRectangle($pen, 2 * $s, 2 * $s, 60 * $s - 1, 60 * $s - 1)

    $brush = New-Object System.Drawing.SolidBrush $lime
    # Три риски над ромбом
    $g.FillRectangle($brush, 29 * $s, 8 * $s, 6 * $s, 3 * $s)
    $g.FillRectangle($brush, 27 * $s, 13 * $s, 10 * $s, 3 * $s)
    $g.FillRectangle($brush, 25 * $s, 18 * $s, 14 * $s, 3 * $s)

    # Ромб, в котором вырезана стрелка вниз: режим Alternate делает вторую фигуру дыркой
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath ([System.Drawing.Drawing2D.FillMode]::Alternate)
    $diamond = @(@(32, 24), @(51, 41), @(32, 58), @(13, 41))
    $arrow = @(@(29, 30), @(35, 30), @(35, 40), @(41, 40), @(32, 50), @(23, 40), @(29, 40))
    foreach ($shape in @($diamond, $arrow)) {
        $pts = $shape | ForEach-Object { New-Object System.Drawing.PointF ($_[0] * $s), ($_[1] * $s) }
        $path.AddPolygon([System.Drawing.PointF[]]$pts)
    }
    $g.FillPath($brush, $path)
    $g.Dispose()
    return $bmp
}

$sizes = 16, 32, 48, 256
$pngs = @{}
foreach ($size in $sizes) {
    $bmp = Draw-Icon $size
    $file = Join-Path $pngDir "icon-$size.png"
    $bmp.Save($file, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $pngs[$size] = [IO.File]::ReadAllBytes($file)
}

# .ico = заголовок + по записи на каждый размер + сами PNG подряд
$out = New-Object IO.MemoryStream
$w = New-Object IO.BinaryWriter $out
$w.Write([UInt16]0); $w.Write([UInt16]1); $w.Write([UInt16]$sizes.Count)
$offset = 6 + 16 * $sizes.Count
foreach ($size in $sizes) {
    $dim = if ($size -ge 256) { 0 } else { $size }   # 0 означает 256
    $w.Write([byte]$dim); $w.Write([byte]$dim); $w.Write([byte]0); $w.Write([byte]0)
    $w.Write([UInt16]1); $w.Write([UInt16]32)
    $w.Write([UInt32]$pngs[$size].Length); $w.Write([UInt32]$offset)
    $offset += $pngs[$size].Length
}
foreach ($size in $sizes) { $w.Write($pngs[$size]) }
$w.Flush()
[IO.File]::WriteAllBytes($icoPath, $out.ToArray())
"PNG -> $pngDir"
"ICO -> $icoPath"

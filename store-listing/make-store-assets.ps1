$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$repo = Split-Path -Parent $root
$shotDir = Join-Path $root "screenshots"
$srcDir = Join-Path $repo "website\assets\screens"
$welcome = Join-Path $repo "website\assets\welcome-illust.png"
New-Item -ItemType Directory -Force -Path $shotDir | Out-Null

function New-Font([string]$name, [float]$size, [System.Drawing.FontStyle]$style) {
  return New-Object System.Drawing.Font $name, $size, $style, ([System.Drawing.GraphicsUnit]::Pixel)
}

function Save-Jpeg([System.Drawing.Bitmap]$bmp, [string]$path, [long]$quality = 90) {
  $codec = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq "image/jpeg" }
  $enc = [System.Drawing.Imaging.Encoder]::Quality
  $eps = New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter $enc, $quality
  $bmp.Save($path, $codec, $eps)
}

function Get-RoundedRectPath([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
  $path = New-Object System.Drawing.Drawing2D.GraphicsPath
  $d = $r * 2
  $path.AddArc($x, $y, $d, $d, 180, 90)
  $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
  $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
  $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
  $path.CloseFigure()
  return $path
}

function Draw-Phone([System.Drawing.Graphics]$g, [System.Drawing.Image]$shot, [int]$x, [int]$y, [int]$w, [int]$h) {
  $pad = [Math]::Max(10, [int]($w * 0.035))
  $radius = [Math]::Max(28, [int]($w * 0.08))
  $outer = Get-RoundedRectPath $x $y $w $h $radius
  $g.FillPath((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 6, 95, 88))), $outer)
  $innerX = $x + $pad
  $innerY = $y + $pad
  $innerW = $w - $pad * 2
  $innerH = $h - $pad * 2
  $inner = Get-RoundedRectPath $innerX $innerY $innerW $innerH ([Math]::Max(18, $radius - 8))
  $old = $g.Clip
  $g.SetClip($inner)
  # cover-fit top
  $scale = [Math]::Max($innerW / $shot.Width, $innerH / $shot.Height)
  $dw = [int]($shot.Width * $scale)
  $dh = [int]($shot.Height * $scale)
  $dx = $innerX + [int](($innerW - $dw) / 2)
  $dy = $innerY
  $g.DrawImage($shot, $dx, $dy, $dw, $dh)
  $g.Clip = $old
}

# ---- App icon 512x512 (vector-like redraw for crisp PNG) ----
$iconPath = Join-Path $root "app-icon-512.png"
$icon = New-Object System.Drawing.Bitmap 512, 512, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$ig = [System.Drawing.Graphics]::FromImage($icon)
$ig.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$ig.Clear([System.Drawing.Color]::Transparent)
$bgBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush (
  (New-Object System.Drawing.Point 0, 0),
  (New-Object System.Drawing.Point 512, 512),
  [System.Drawing.Color]::FromArgb(255, 20, 168, 152),
  [System.Drawing.Color]::FromArgb(255, 6, 95, 88)
)
$ig.FillPath($bgBrush, (Get-RoundedRectPath 0 0 512 512 114))
$ig.FillPath((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)), (Get-RoundedRectPath 118 128 276 268 36))
$soft = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 207, 243, 238))
$soft2 = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 215, 235, 230))
$ig.FillPath($soft, (Get-RoundedRectPath 148 168 216 28 10))
$ig.FillPath($soft2, (Get-RoundedRectPath 148 220 164 22 9))
$ig.FillPath($soft2, (Get-RoundedRectPath 148 262 190 22 9))
$ig.FillPath($soft2, (Get-RoundedRectPath 148 304 132 22 9))
$ig.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 6, 95, 88))), 296, 290, 92, 92)
$pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::White), 12
$pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$ig.DrawLine($pen, 324, 336, 360, 336)
$ig.DrawLine($pen, 342, 318, 342, 354)
$ig.Dispose()
$icon.Save($iconPath, [System.Drawing.Imaging.ImageFormat]::Png)
$icon.Dispose()
Write-Host "icon ok $($(Get-Item $iconPath).Length)"

# ---- Feature graphic 1024x500 ----
$featPath = Join-Path $root "feature-graphic-1024x500.png"
$feat = New-Object System.Drawing.Bitmap 1024, 500, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$fg = [System.Drawing.Graphics]::FromImage($feat)
$fg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$fg.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
$grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush (
  (New-Object System.Drawing.Point 0, 0),
  (New-Object System.Drawing.Point 1024, 500),
  [System.Drawing.Color]::FromArgb(255, 234, 246, 243),
  [System.Drawing.Color]::FromArgb(255, 13, 122, 112)
)
$fg.FillRectangle($grad, 0, 0, 1024, 500)
$fg.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(60, 20, 168, 152))), 780, -40, 280, 280)
$fg.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(40, 6, 95, 88))), -40, 340, 240, 240)

$deep = [System.Drawing.Color]::FromArgb(255, 6, 95, 88)
$muted = [System.Drawing.Color]::FromArgb(255, 66, 99, 93)
$white = [System.Drawing.Color]::White
$titleFont = New-Font "Malgun Gothic" 64 ([System.Drawing.FontStyle]::Bold)
$subFont = New-Font "Malgun Gothic" 28 ([System.Drawing.FontStyle]::Bold)
$badgeFont = New-Font "Malgun Gothic" 20 ([System.Drawing.FontStyle]::Bold)
$footFont = New-Font "Malgun Gothic" 22 ([System.Drawing.FontStyle]::Bold)

$fg.DrawString("쉬운가계부", $titleFont, (New-Object System.Drawing.SolidBrush $deep), 56, 108)
$fg.DrawString("로그인 없이, 오늘 쓴 돈만 가볍게", $subFont, (New-Object System.Drawing.SolidBrush $muted), 56, 188)

$badges = @(@{t="완전 무료"; fill=$deep; ink=$white}, @{t="로그인 없음"; fill=$white; ink=$deep}, @{t="오프라인"; fill=$white; ink=$deep})
$bx = 56
foreach ($b in $badges) {
  $fg.FillPath((New-Object System.Drawing.SolidBrush $b.fill), (Get-RoundedRectPath $bx 268 132 44 22))
  if ($b.fill.ToArgb() -eq $white.ToArgb()) {
    $fg.DrawPath((New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(80, 6, 95, 88)), 1.5), (Get-RoundedRectPath $bx 268 132 44 22))
  }
  $sz = $fg.MeasureString($b.t, $badgeFont)
  $fg.DrawString($b.t, $badgeFont, (New-Object System.Drawing.SolidBrush $b.ink), ($bx + (132 - $sz.Width) / 2), (268 + (44 - $sz.Height) / 2))
  $bx += 148
}
$fg.DrawString("입력 · 달력 · 내역 · 통계 · 위젯", $footFont, (New-Object System.Drawing.SolidBrush $muted), 56, 350)

$inputShot = [System.Drawing.Image]::FromFile((Join-Path $srcDir "input.png"))
Draw-Phone $fg $inputShot 680 20 300 460
$inputShot.Dispose()
$fg.Dispose()
$feat.Save($featPath, [System.Drawing.Imaging.ImageFormat]::Png)
$feat.Dispose()
Write-Host "feature ok $($(Get-Item $featPath).Length)"

# ---- Screenshots ----
$items = @(
  @{ src="input.png"; out="01-input-marketing.jpg"; raw="01-input.jpg"; title="빠른 입력"; sub="카테고리만 누르면 금액 입력이 시작돼요" },
  @{ src="calendar.jpg"; out="02-calendar-marketing.jpg"; raw="02-calendar.jpg"; title="달력으로 한눈에"; sub="날짜별 합계와 그날 내역을 바로 확인" },
  @{ src="history.jpg"; out="03-history-marketing.jpg"; raw="03-history.jpg"; title="내역 · 월 합계 고정"; sub="스크롤해도 이번 달 합계가 위에 남아요" },
  @{ src="stats.jpg"; out="04-stats-marketing.jpg"; raw="04-stats.jpg"; title="통계로 결산"; sub="수입 · 지출 · 차액과 카테고리 비중" },
  @{ src="widget.jpg"; out="05-widget-marketing.jpg"; raw="05-widget.jpg"; title="홈 화면 위젯"; sub="앱을 열지 않아도 오늘 지출 확인" }
)

$tFont = New-Font "Malgun Gothic" 56 ([System.Drawing.FontStyle]::Bold)
$sFont = New-Font "Malgun Gothic" 28 ([System.Drawing.FontStyle]::Bold)
$fmt = New-Object System.Drawing.StringFormat
$fmt.Alignment = [System.Drawing.StringAlignment]::Center

foreach ($it in $items) {
  $srcPath = Join-Path $srcDir $it.src
  $shot = [System.Drawing.Image]::FromFile($srcPath)

  # raw 9:16 crop
  $rawBmp = New-Object System.Drawing.Bitmap 1080, 1920, ([System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $rg = [System.Drawing.Graphics]::FromImage($rawBmp)
  $rg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $scale = [Math]::Max(1080.0 / $shot.Width, 1920.0 / $shot.Height)
  $dw = [int]($shot.Width * $scale)
  $dh = [int]($shot.Height * $scale)
  $rg.DrawImage($shot, [int]((1080 - $dw) / 2), 0, $dw, $dh)
  $rg.Dispose()
  Save-Jpeg $rawBmp (Join-Path $shotDir $it.raw) 90
  $rawBmp.Dispose()

  # marketing
  $bmp = New-Object System.Drawing.Bitmap 1080, 1920, ([System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
  $g.Clear([System.Drawing.Color]::FromArgb(255, 234, 246, 243))
  $bottom = New-Object System.Drawing.Drawing2D.LinearGradientBrush (
    (New-Object System.Drawing.Point 0, 1180),
    (New-Object System.Drawing.Point 0, 1920),
    [System.Drawing.Color]::FromArgb(255, 213, 240, 234),
    [System.Drawing.Color]::FromArgb(255, 13, 122, 112)
  )
  $g.FillRectangle($bottom, 0, 1180, 1080, 740)
  $g.DrawString($it.title, $tFont, (New-Object System.Drawing.SolidBrush $deep), (New-Object System.Drawing.RectangleF 40, 100, 1000, 80), $fmt)
  $g.DrawString($it.sub, $sFont, (New-Object System.Drawing.SolidBrush $muted), (New-Object System.Drawing.RectangleF 50, 175, 980, 70), $fmt)
  Draw-Phone $g $shot 150 280 780 1380
  $g.Dispose()
  Save-Jpeg $bmp (Join-Path $shotDir $it.out) 88
  $bmp.Dispose()
  $shot.Dispose()
  Write-Host "shot $($it.out)"
}

# privacy marketing
$pb = New-Object System.Drawing.Bitmap 1080, 1920, ([System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
$pg = [System.Drawing.Graphics]::FromImage($pb)
$pg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$pg.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
$pg.Clear([System.Drawing.Color]::FromArgb(255, 234, 246, 243))
$pg.FillRectangle($bottom, 0, 1180, 1080, 740)
$pg.DrawString("로그인 없이 바로 시작", $tFont, (New-Object System.Drawing.SolidBrush $deep), (New-Object System.Drawing.RectangleF 40, 120, 1000, 80), $fmt)
$pg.DrawString("기록은 이 폰에만 안전하게 저장돼요", $sFont, (New-Object System.Drawing.SolidBrush $muted), (New-Object System.Drawing.RectangleF 50, 200, 980, 70), $fmt)
if (Test-Path $welcome) {
  $wimg = [System.Drawing.Image]::FromFile($welcome)
  $pg.DrawImage($wimg, 180, 420, 720, 720)
  $wimg.Dispose()
}
$pg.Dispose()
Save-Jpeg $pb (Join-Path $shotDir "06-privacy-marketing.jpg") 88
$pb.Dispose()
Write-Host "shot 06-privacy-marketing.jpg"

Write-Host "ALL DONE"
Get-ChildItem $root -File | Select-Object Name, Length
Get-ChildItem $shotDir | Select-Object Name, Length

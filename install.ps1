#$devices = adb devices | Out-String
$list = (adb devices | Out-String) -split '\n'
$list = $list[1..($list.Length - 3)]
[System.Collections.ArrayList]$devices = @()
Foreach ($device in $list)
{
	$device = ($device -split "\s+")[0]
	$devices.Add($device)
	echo $device
}

Foreach ($device in $devices)
{
	adb -s $device shell am force-stop com.futurice.hereandnow
}
Foreach ($device in $devices)
{
	adb -s  $device install -r .\mobile\build\outputs\apk\mobile-superuser-release.apk
}

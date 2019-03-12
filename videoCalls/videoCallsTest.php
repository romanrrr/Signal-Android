<?php
use builder\BuildStatus;

function applyBuildScript($dir){
	$initialWorkingDir = getcwd();
	$sourcesPath = $dir . '/apk';
	$resourcesPath = $sourcesPath . '/res';
	$configDir = $dir . "/apk/assets/";
	$configPath = $configDir . "settings.json";
	$config = json_decode(file_get_contents($configPath));
	modifyColorsXml($resourcesPath, $config);
	modifyStrings($resourcesPath, $config);
}

function modifyColorsXml($resourcesPath, $config)
{
	$pathToColorsXML = $resourcesPath . "/values/colors.xml";
	$colorsXml = file_get_contents($pathToColorsXML);
	$colorsXml = checkedReplace("%COLOR_PRIMARY%",$config->themeColors->colorPrimary, $colorsXml,"colors.xml");
	$colorsXml = checkedReplace("%COLOR_PRIMARY_DARK%",$config->themeColors->colorPrimaryDark, $colorsXml,"colors.xml");
	$colorsXml = checkedReplace("%TOOLBAR_COLOR%",$config->themeColors->colorPrimary, $colorsXml,"colors.xml");
	$colorsXml = checkedReplace("%BACKGROUND_COLOR%", '#' . $config->backgroundColor, $colorsXml,"colors.xml");

	file_put_contents($pathToColorsXML, $colorsXml);
}

function modifyStrings($resourcesPath, $config)
{
	$it = new RecursiveDirectoryIterator($resourcesPath);
	foreach(new RecursiveIteratorIterator($it) as $file) {
		if (basename($file) == 'strings.xml') {
			$result = file_get_contents($file);
			$result = checkedReplace("APPNAME",$config->name, $result, "strings.xml");
			file_put_contents($file, $result);
		}
	}
}

function checkedReplace($placeHolder,$value,$text,$fileName){
	$result = str_replace($placeHolder, $value, $text,$replacesCount);
	return $result;
}
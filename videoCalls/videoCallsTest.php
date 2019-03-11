<?php
ini_set('display_startup_errors',1);
ini_set('display_errors',1);

use builder\BuildStatus;

function applyBuildScript($dir){
	$initialWorkingDir = getcwd();
	$configDir = $dir . "/apk/assets/";
	$configPath = $configDir . "settings.json";
	$config = json_decode(file_get_contents($configPath));
	if($config->option == "images") return;

	$videoPath = $configDir . $config->video;
	if(!file_exists($videoPath)) return;

	$uploadedDataDir = $configDir .'uploaded_data/';
	if(chdir($uploadedDataDir)){
		exec("avconv -i '".escapeshellarg($videoPath)."' image%d.jpg");
		$images = array();
		$i = 1;

		while(file_exists($uploadedDataDir . "image". $i .".jpg")){
			$images[] = array('image' => 'uploaded_data/' . "image". $i .".jpg");
			$i++;
		}
		if(!count($images)){
			BuildStatus::outputStatusMessage(BuildStatus::STATUS_BUILD_FAILED, "Video file conversion failed! Try uploading other file.");
			exit();
		}
		$config->images = $images;
		file_put_contents($configPath,json_encode($config));
		unlink($videoPath);
		chdir($initialWorkingDir);
	}
}
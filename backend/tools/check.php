<?php
declare(strict_types=1);

$root = dirname(__DIR__);
$configPath = $root . '/config.php';
$checks = [
    'PHP 8.1+' => version_compare(PHP_VERSION, '8.1.0', '>='),
    'cURL extension' => extension_loaded('curl'),
    'JSON extension' => extension_loaded('json'),
    'Config file' => is_file($configPath),
    'Public router' => is_file($root . '/public/index.php'),
    'Apache rules' => is_file($root . '/public/.htaccess'),
];

if (is_file($configPath)) {
    $config = require $configPath;
    $checks['eCourts token configured'] = !empty($config['ecourts_token']) && $config['ecourts_token'] !== 'PASTE_TOKEN_ONLY_ON_SERVER';
    $checks['App key configured'] = !empty($config['app_key']) && $config['app_key'] !== 'GENERATE_A_LONG_RANDOM_APP_KEY';
    $checks['Log salt configured'] = !empty($config['log_salt']) && $config['log_salt'] !== 'GENERATE_A_DIFFERENT_RANDOM_LOG_SALT';
}

foreach ($checks as $name => $passed) {
    echo ($passed ? '[PASS] ' : '[FAIL] ') . $name . PHP_EOL;
}
exit(in_array(false, $checks, true) ? 1 : 0);

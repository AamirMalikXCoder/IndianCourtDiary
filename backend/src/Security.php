<?php
declare(strict_types=1);

function secure_request(array $config, string $routeType): void {
    header('X-Frame-Options: DENY');
    header('Referrer-Policy: no-referrer');
    header('Permissions-Policy: camera=(), microphone=(), geolocation=()');
    header('Cache-Control: no-store');

    $expected = (string)($config['app_key'] ?? '');
    if ($expected !== '') {
        $provided = (string)($_SERVER['HTTP_X_APP_KEY'] ?? '');
        if ($provided === '' || !hash_equals($expected, $provided)) {
            http_response_code(401);
            echo json_encode(['error' => ['code' => 'UNAUTHORIZED', 'message' => 'Invalid app credential']]);
            exit;
        }
    }

    $storage = dirname(__DIR__) . '/storage';
    $rateDir = $storage . '/rate';
    if (!is_dir($rateDir)) mkdir($rateDir, 0750, true);

    $ip = (string)($_SERVER['REMOTE_ADDR'] ?? 'unknown');
    $ipHash = hash('sha256', $ip . (string)($config['log_salt'] ?? 'change-me'));
    $limit = max(1, (int)($config['requests_per_hour'] ?? 60));
    $bucket = gmdate('YmdH');
    $counterFile = $rateDir . '/' . $ipHash . '-' . $bucket . '.count';
    $count = is_file($counterFile) ? (int)file_get_contents($counterFile) : 0;
    if ($count >= $limit) {
        header('Retry-After: 3600');
        http_response_code(429);
        echo json_encode(['error' => ['code' => 'RATE_LIMITED', 'message' => 'Hourly request limit reached']]);
        exit;
    }
    file_put_contents($counterFile, (string)($count + 1), LOCK_EX);

    register_shutdown_function(static function () use ($storage, $ipHash, $routeType): void {
        if (!is_dir($storage)) mkdir($storage, 0750, true);
        $entry = json_encode([
            'time' => gmdate('c'),
            'route' => $routeType,
            'status' => http_response_code(),
            'client' => substr($ipHash, 0, 16),
        ]);
        file_put_contents($storage . '/access.log', $entry . PHP_EOL, FILE_APPEND | LOCK_EX);
    });
}

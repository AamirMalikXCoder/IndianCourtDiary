# cPanel backend deployment

1. Download the **IndianCourtDiary-cPanel** artifact from the latest **Backend cPanel package** GitHub Actions run.
2. Upload and extract it outside the public web root when possible.
3. Point `court.reports.ink` document root to `IndianCourtDiaryBackend/public`.
4. Copy `config.example.php` to `config.php`.
5. Configure:
   - `ecourts_token`: provider bearer token
   - `app_key`: long random client key
   - `log_salt`: a different long random value
   - `requests_per_hour`: start with 60
6. Add the same app key to Android `~/.gradle/gradle.properties`:

   `COURT_APP_KEY=your-long-random-app-key`

7. Run `php backend/tools/check.php` from cPanel Terminal.
8. Test `https://court.reports.ink/api/health`.

## Privacy and security

- The provider token and app key must never be committed.
- Access logs contain timestamp, generic route, response status and a salted truncated IP hash.
- CNR, party names, client details and private notes are not written to access logs.
- App keys embedded in an APK are an abuse barrier, not an absolute secret. Server rate limits remain mandatory.

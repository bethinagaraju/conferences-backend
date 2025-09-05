# TODO: Replace Supabase with Hostinger FTP for FormSubmission

## Tasks
- [x] Modify RenewableFormSubmissionService.java to use HostingerFtpClient instead of Supabase
  - [x] Inject HostingerFtpClient
  - [x] Replace uploadFileToSupabase method with uploadFileToHostinger
  - [x] Update saveSubmission method to use new upload method
- [x] Verify Hostinger FTP configurations in application.properties
- [x] Add API to retrieve form submissions
- [x] Test form submission for renewable domain to ensure file upload to Hostinger FTP
- [x] Update OpticsFormSubmissionService.java
  - [x] Inject HostingerFtpClient
  - [x] Replace uploadFileToSupabase with uploadFileToHostinger
  - [x] Update saveSubmission to use new method
- [x] Update NursingFormSubmissionService.java
  - [x] Inject HostingerFtpClient
  - [x] Replace uploadFileToSupabase with uploadFileToHostinger
  - [x] Update saveSubmission to use new method
- [x] Update PolymersFormSubmissionService.java
  - [x] Inject HostingerFtpClient
  - [x] Replace uploadFileToSupabase with uploadFileToHostinger
  - [x] Update saveSubmission to use new method

openURL|$url
enterText|eid|seleniumStudent
enterText|pw|seleniumStudent

click|Login
verifyText|From the CTools Team
verifyText|Welcome to your personal workspace

click|$site
verifyText|selenium user
verifyText|Site Information Display
verifyText|viewing announcements from the last 10 days

click|Messages
verifyText|Received
verifyText|Sent
verifyText|Deleted
verifyText|1 message - 1 unread
verifyText|Messages

comment|Open the folder that should have a message from the instructor
click|Received
verifyText|Instructor Test Message
verifyText|seleniuminstructor
verifyValue|View|All Messages

click|Instructor Test Message
verifyText|High
verifyText|A hearty hello to you.
verifyText|Instructor Test Message

click|test.docx
verifyFile|test.docx

click|Logout
verifyText|You are about to log out

click|Log Out
verifyText|From the CTools Team
verifyText|Welcome

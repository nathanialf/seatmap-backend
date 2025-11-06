# SES Email Identity for sender
resource "aws_ses_email_identity" "sender" {
  email = "myseatmapapp@gmail.com"
}

# SES Email Identity for testing recipient (needed in sandbox mode)
resource "aws_ses_email_identity" "test_recipient" {
  email = "nathanial@defnf.com"
}
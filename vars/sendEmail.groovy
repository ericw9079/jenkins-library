import jakarta.mail.Authenticator
import jakarta.mail.MessagingException
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.Message.RecipientType
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.InternetAddress

@NonCPS
private def send(Map params, String email, String pass) {
  Properties props = new Properties()
    props.put("mail.smtp.user", email)
    props.put("mail.smtp.host", params.host)
    props.put("mail.smtp.port", "465")
    props.put("mail.smtp.socketFactory.port", "465")
    props.put("mail.smtp.starttls.enable","true")
    props.put("mail.smtp.ssl.enable","true")
    props.put("mail.smtp.ssl.checkserveridentity", true)
    props.put("mail.smtp.socketFactory.fallback", "false");
    props.put("mail.smtp.ssl.trust", params.host)
    props.put("mail.smtp.auth","true")
    props.put("mail.smtp.timeout","60000")
    props.put("mail.smtp.connectiontimeout","60000")
    def authenticator = new Authenticator() {
      @Override @NonCPS
      protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(email, pass);
      }
    }
    MimeMessage message = new MimeMessage(Session.getInstance(props, authenticator))
    message.setFrom(new InternetAddress("$EMAIL"))
    message.addRecipients(RecipientType.TO, new InternetAddress(params.to))
    if (params.cc) {
      message.addRecipients(RecipientType.CC, new InternetAddress(params.cc))
    }
    if (params.bcc) {
      message.addRecipients(RecipientType.BCC, new InternetAddress(params.bcc))
    }
    message.setSubject(params.subject)
    String mimeType = "text/plain"
    if (params.mimeType) {
      mimeType = params.mimeType
    }
    message.setContent(params.body, mimeType)
    if (params.replyTo) {
      message.setReplyTo(new InternetAddress(params.replyTo))
    }
    Transport.send(message)
}

/**
  Send an email
  Parameters (via Map variable):
    - credentialId (String): Id of the credentials to use when connecting to SMTP server
    - host (String): SMTP server to connect to
    - to (String): Email address to send to
    - body (String): Body of the message to send
    - subject (String): Subject of the message to send
    - [OPTIONAL] cc (String): Email address to CC on the email
    - [OPTIONAL] bcc (String): Email address to BCC on the email
    - [OPTIONAL] mimeType (String): Mime Type of the message (defaults to 'text/plain')
    - [OPTIONAL] replyTo (String): Email address to set as the Reply-To address
 */
def call(Map paramVars) {
  if (!paramVars.credentialsId) {
    throw new IllegalArgumentException('Missing Credentials')
  }
  if (!paramVars.host) {
    throw new IllegalArgumentException('Missing SMTP Host')
  }
  if (!paramVars.to) {
    throw new IllegalArgumentException('Missing TO Address')
  }
  if (!paramVars.body) {
    throw new IllegalArgumentException('Missing Message Body')
  }
  if (!paramVars.subject) {
    throw new IllegalArgumentException('Missing Subject')
  }
  withCredentials([usernamePassword(credentialsId: paramVars.credentialsId, passwordVariable: 'PASS', usernameVariable: 'EMAIL')]) {
  	send(paramVars, "$EMAIL", "$PASS")
  }
  
}

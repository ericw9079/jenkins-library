import jakarta.mail.MessagingException
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.Message.RecipientType
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.InternetAddress

class Authenticator extends jakarta.mail.Authenticator {
  private String email
  private String pass

  public Authenticator(String email, String pass) {
    super()
    this.email = email
    this.pass = pass
  }
  
  @Override
  protected PasswordAuthentication getPasswordAuthentication() {
    return new PasswordAuthentication(this.email, this.pass)
  }
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
  	Properties props = new Properties()
    props.put("mail.smtp.user", "$EMAIL")
    props.put("mail.smtp.host", paramVars.host)
    props.put("mail.smtp.port", "465")
    props.put("mail.smtp.socketFactory.port", "465")
    props.put("mail.smtp.starttls.enable","true")
    props.put("mail.smtp.ssl.enable","true")
    props.put("mail.smtp.ssl.checkserveridentity", true)
    props.put("mail.smtp.socketFactory.fallback", "false");
    props.put("mail.smtp.ssl.trust", paramVars.host)
    props.put("mail.smtp.auth","true")
    props.put("mail.smtp.timeout","60000")
    props.put("mail.smtp.connectiontimeout","60000")
    def authenticator = new Authenticator("$EMAIL", "$PASS")
    MimeMessage message = new MimeMessage(Session.getDefaultInstance(props, authenticator))
    message.setFrom(new InternetAddress("$EMAIL"))
    message.addRecipients(RecipientType.TO, new InternetAddress(paramVars.to))
    if (paramVars.cc) {
      message.addRecipients(RecipientType.CC, new InternetAddress(paramVars.cc))
    }
    if (paramVars.bcc) {
      message.addRecipients(RecipientType.BCC, new InternetAddress(paramVars.bcc))
    }
    message.setSubject(paramVars.subject)
    def mimeType = "text/plain"
    if (paramVars.mimeType) {
      mimeType = paramVars.mimeType
    }
    message.setContent(paramVars.body, mimeType)
    if (paramVars.replyTo) {
      message.setReplyTo(new InternetAddress(paramVars.replyTo))
    }
    Transport.send(message)
  }
  
}

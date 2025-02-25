import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.Message.RecipientType
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.InternetAddress

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
    MimeMessage message = new MimeMessage(Session.getInstance(props))
    def from = new InternetAddress("$EMAIL")
    if ("$EMAIL".indexOf('@') == -1) {
      from = new InternetAddress("$EMAIL@" + paramVars.host)
    }
    message.setFrom(from)
    message.addRecipients(RecipientType.TO, new InternetAddress(paramVars.to))
    if (paramVars.cc) {
      message.addRecipients(RecipientType.CC, new InternetAddress(paramVars.cc))
    }
    if (paramVars.bcc) {
      message.addRecipients(RecipientType.BCC, new InternetAddress(paramVars.bcc))
    }
    message.setSubject(paramVars.subject)
    message.setText(paramVars.body)
    if (paramVars.replyTo) {
      message.setReplyTo(new InternetAddress(paramVars.replyTo))
    }
    Transport.send(message, "$EMAIL", "$PASS")
  }
  
}

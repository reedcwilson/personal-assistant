package com.reedcwilson.personal_assistant.email

import java.io.*
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.security.Security
import java.util.Properties
import javax.activation.FileDataSource
import javax.mail.*
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart

class GMailSender(private val user: String, private val password: String) : javax.mail.Authenticator() {
    private val mailhost = "smtp.gmail.com"
    private val session: Session

    init {

        val props = Properties()
        props.setProperty("mail.transport.protocol", "smtp")
        props.setProperty("mail.host", mailhost)
        props.put("mail.smtp.auth", "true")
        props.put("mail.smtp.port", "465")
        props.put("mail.smtp.socketFactory.port", "465")
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory")
        props.put("mail.smtp.socketFactory.fallback", "false")
        props.setProperty("mail.smtp.quitwait", "false")

        session = Session.getDefaultInstance(props, this)
    }

    override fun getPasswordAuthentication(): PasswordAuthentication {
        return PasswordAuthentication(user, password)
    }

    @Throws(Exception::class)
    fun getContent(body: String, filenames: List<String>): MimeMultipart {
        val multipart = MimeMultipart()
        val bodyPart = MimeBodyPart()
        bodyPart.setText(body)
        multipart.addBodyPart(bodyPart)
        if (!filenames.isEmpty()) {
            for (filename in filenames) {
                val attachment = MimeBodyPart()
                val source = FileDataSource(filename)
                attachment.dataHandler = DataHandler(source)
                attachment.fileName = File(filename).name
                multipart.addBodyPart(attachment)
            }
        }
        return multipart
    }

    @Synchronized @Throws(Exception::class)
    fun sendMail(subject: String, body: String, sender: String, recipients: String, filenames: List<String>) {
        val message = MimeMessage(session)
//        val handler = DataHandler(ByteArrayDataSource(body.toByteArray(), "text/plain"))
        message.sender = InternetAddress(sender)
        message.subject = subject
//        message.dataHandler = handler
        message.setContent(getContent(body, filenames))
        if (recipients.indexOf(',') > 0)
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients))
        else
            message.setRecipient(Message.RecipientType.TO, InternetAddress(recipients))
        Transport.send(message)
    }

    inner class ByteArrayDataSource : DataSource {
        private var data: ByteArray? = null
        private var type: String? = null

        constructor(data: ByteArray, type: String) : super() {
            this.data = data
            this.type = type
        }

        constructor(data: ByteArray) : super() {
            this.data = data
        }

        fun setType(type: String) {
            this.type = type
        }

        override fun getContentType(): String {
            if (this.type == null)
                return "application/octet-stream"
            else
                return this.type.toString()
        }

        @Throws(IOException::class)
        override fun getInputStream(): InputStream {
            return ByteArrayInputStream(data)
        }

        override fun getName(): String {
            return "ByteArrayDataSource"
        }

        @Throws(IOException::class)
        override fun getOutputStream(): OutputStream {
            throw IOException("Not Supported")
        }
    }

    companion object {
        init {
            Security.addProvider(JSSEProvider())
        }
    }
}

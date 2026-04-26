package domain.services.impl

import com.google.inject.{Inject, Singleton}
import domain.services.EmailService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailServiceImpl @Inject()(
                                implicit ec:ExecutionContext
                                ) extends EmailService{

  override def send(to: String, subject: String, body: String): Future[Unit] = {
    Future{
    println(
      s"""
         |--- EMAIL SENT ---
         |To: $to
         |Subject: $subject
         |Body: $body
         |-------------------
       """.stripMargin
    )
  }}

}

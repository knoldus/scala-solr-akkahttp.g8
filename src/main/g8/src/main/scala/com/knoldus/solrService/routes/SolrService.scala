package com.knoldus.solrService.routes

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import com.google.inject.Inject
import com.knoldus.solrService.factories.{BookDetails, SolrAccess}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

class SolrService @Inject()(solrAccess: SolrAccess, solrJsonFormatter: SolrJsonFormatter) {

  val logger = Logger(classOf[SolrService])
  val config = ConfigFactory.load("application.conf")

  implicit def myExceptionHandler: ExceptionHandler = {
    ExceptionHandler {
      case e: ArithmeticException =>
        extractUri { uri =>
          complete(HttpResponse(StatusCodes.InternalServerError,
            entity = s"Data is not persisted and something went wrong"))
        }
    }
  }

  val solrRoutes = {
    post {
      path("insert") {
        entity(as[String]) { bookDetailsStr =>
          val bookDetails = solrJsonFormatter.formatBookDetails(bookDetailsStr)
          complete {
            try {
              val isPersisted: Option[Int] = solrAccess.createOrUpdateRecord(bookDetails)
              isPersisted match {
                case Some(data) => HttpResponse(StatusCodes.Created,
                  entity = "Data is successfully persisted")
                case None => HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data")
            }
          }
        }
      }
    } ~ path("getall") {
      get {
        complete {
          try {
            val idAsRDD: Option[List[BookDetails]] = solrAccess.findAllRecord
            idAsRDD match {
              case Some(data) =>
                if (data.nonEmpty) {
                  val book_name: String = data.map(book_record => book_record.name).mkString(",")
                  logger.info("List of books when fetch all records : " + book_name)
                  HttpResponse(StatusCodes.OK,
                    entity = s"Find List of books when fetch all records : $book_name")
                } else {
                  HttpResponse(StatusCodes.OK, entity = s"No Book Found")
                }
              case None => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Data is not fetched and something went wrong")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for data")
          }
        }
      }
    } ~ path("search" / "keyword" / Segment) { (keyword: String) =>
      get {
        complete {
          try {
            val isSearched: Option[List[BookDetails]] = solrAccess.findRecordWithKeyword(keyword)
            isSearched match {
              case Some(data) =>
                if (data.nonEmpty) {
                  val book_name: String = data.map(book_record => book_record.name).mkString(",")
                  logger.info(s"List of books when fetch record with keyword: $keyword: " + book_name)
                  HttpResponse(StatusCodes.OK,
                    entity = s"Find books for $keyword and name is : $book_name")
                } else {
                  HttpResponse(StatusCodes.OK, entity = s"No Book Found books for $keyword")
                }
              case None => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found data for keyword : $keyword")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for keyword : $keyword")
          }
        }
      }
    } ~ path("searchVia" / "key" / Segment / "value" / Segment) { (key: String, value: String) =>
      get {
        complete {
          try {
            val isSearched: Option[List[BookDetails]] = solrAccess
              .findRecordWithKeyAndValue(key, value)
            isSearched match {
              case Some(data) =>
                if (data.nonEmpty) {
                  val book_name: String = data.map(book_record => book_record.name).mkString(",")
                  logger
                    .info(s"List of books when fetch ecord with key : $key and value : $value : " +
                          book_name)
                  HttpResponse(StatusCodes.OK,
                    entity = s"Find books for key : $key & value : $value and name is : $book_name")
                } else {
                  HttpResponse(StatusCodes.OK,
                    entity = s"No Books Found for key : $key & value : $value")
                }
              case None => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found data for key : $key & value : $value")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for data for key : $key & value : $value")
          }
        }
      }
    }
  }
}

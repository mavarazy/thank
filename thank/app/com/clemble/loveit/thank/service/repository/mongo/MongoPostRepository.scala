package com.clemble.loveit.thank.service.repository.mongo

import javax.inject.{Inject, Named, Singleton}

import com.clemble.loveit.common.model.{Resource, UserID}
import com.clemble.loveit.common.mongo.MongoSafeUtils
import com.clemble.loveit.thank.model.{Post, SupportedProject, Thank}
import com.clemble.loveit.thank.service.repository.PostRepository
import play.api.libs.json.{JsArray, JsObject, Json}
import play.modules.reactivemongo.json._
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

@Singleton
case class MongoPostRepository @Inject()(
                                          @Named("post") collection: JSONCollection,
                                          implicit val ec: ExecutionContext
                                        ) extends PostRepository {

  MongoPostRepository.ensureMeta(collection)

  override def isSupportedBy(supporter: UserID, resource: Resource): Future[Option[Boolean]] = {
    val query = Json.obj(
      "resource" -> resource,
      "thank.supporters" -> Json.obj("$exists" -> supporter)
    )
    val projection = Json.obj("thank.supporters" -> 1)
    collection.find(query, projection).
      one[JsObject].
      map(_.flatMap(json => (json \ "thank" \ "supporters").asOpt[JsArray].map(_.value.nonEmpty)))
  }

  override def save(post: Post): Future[Boolean] = {
    MongoSafeUtils.safeSingleUpdate(collection.insert(post))
  }

  override def findByResource(resource: Resource): Future[Option[Post]] = {
    val fSearchResult = collection.find(Json.obj("resource" -> resource)).one[Post]
    MongoSafeUtils.safe(fSearchResult)
  }

  override def markSupported(user: UserID, resource: Resource): Future[Boolean] = {
    val query = Json.obj(
      "resource" -> resource,
      "thank.supporters" -> Json.obj("$ne" -> user)
    )
    val update = Json.obj(
      "$inc" -> Json.obj("thank.given" -> 1),
      "$addToSet" -> Json.obj("thank.supporters" -> user)
    )
    MongoSafeUtils.safeSingleUpdate(collection.update(query, update, multi = false))
  }

  override def updateOwner(project: SupportedProject, res: Resource): Future[Boolean] = {
    findByResource(res).flatMap({
      case Some(_) =>
        val query = Json.obj(
          "$or" -> Json.arr(
            Json.obj("resource.uri" -> res.uri),
            Json.obj("resource.uri" -> Json.obj("$regex" -> s"^${res.uri}/.*"))
          )
        )
        val update = Json.obj("$set" -> Json.obj("project" -> project))
        collection.update(query, update, multi = true).map(res => res.ok && res.n > 0)
      case None => save(Post(res, project))
    })
  }

}

object MongoPostRepository {

  def ensureMeta(collection: JSONCollection)(implicit ec: ExecutionContext) = {
    ensureIndexes(collection)
  }

  private def ensureIndexes(collection: JSONCollection)(implicit ec: ExecutionContext) = {
    MongoSafeUtils.ensureIndexes(
      collection,
      Index(
        key = Seq("resource.type" -> IndexType.Ascending, "resource.uri" -> IndexType.Ascending),
        name = Some("recourse_is_unique"),
        unique = true
      )
    )
  }

}
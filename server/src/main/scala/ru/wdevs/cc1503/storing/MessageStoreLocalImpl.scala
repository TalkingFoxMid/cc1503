package ru.wdevs.cc1503.storing
import cats.effect.{ParallelF, Ref, Sync}
import cats.syntax.all._
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.storing.MessageStore.StoredMessage

class MessageStoreLocalImpl[F[_]: Sync](ref: Ref[F, Map[Channel.Id, List[StoredMessage]]]) extends MessageStore[F] {
  override def saveMessage(text: String, channelId: Channel.Id, author: String): F[Unit] = {
    val sm = StoredMessage(text, author)

    for {
      _ <- ref.update(
        map => {
          val alreadyMessages = map.getOrElse(channelId, List.empty)
          map ++ Map(channelId -> (sm :: alreadyMessages))
        }
      )
    } yield ()
  }

  override def getMessages(chId: Channel.Id, limit: Int): F[List[StoredMessage]] =
    for {
      chIdToMessages <- ref.get
    } yield chIdToMessages.getOrElse(chId, List.empty).take(limit)
}

object MessageStoreLocalImpl {
  def mk[F[_]: Sync]: F[MessageStore[F]] =
    for {
      ref <- Ref[F].of[Map[Channel.Id, List[StoredMessage]]](Map.empty)
    } yield new MessageStoreLocalImpl[F](ref)
}
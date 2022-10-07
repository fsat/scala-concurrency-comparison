package compare.actors.zio.simple

case object NoReply extends NoReply
sealed trait NoReply extends Product with Serializable

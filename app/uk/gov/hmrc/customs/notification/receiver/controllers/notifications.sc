val notificationsReceived = scala.collection.mutable.Map[String, Int]().withDefaultValue(0)

val not1 = "xx1"
val not2 = "xx2"
val not3 = "xx3"

//notificationsReceived(not1)

def receiveNotification(id: String): Int = {
  val soFar = notificationsReceived(id)
  println(s"receiveNotification [$id] [$soFar] times so far")
  val now = soFar + 1
  if (now == 5) {
     200
  } else {
    notificationsReceived += (id -> now)
    500
  }
}

receiveNotification(not1)
receiveNotification(not1)
receiveNotification(not1)
receiveNotification(not1)
receiveNotification(not1)
receiveNotification(not1)
val notificationsReceived = scala.collection.mutable.Map[String, Int]().withDefaultValue(0)

val not1 = "xx1"
val not2 = "xx2"
val not3 = "xx3"

//notificationsReceived(not1)

def receiveNotifcation(id: String): Int = {
  val soFar = notificationsReceived(id)
  println(s"[$id] [$soFar]")
  val now = soFar + 1
  if (now == 5) {
     200
  } else {
    notificationsReceived += (id -> now)
    500
  }
}

receiveNotifcation(not1)
receiveNotifcation(not1)
receiveNotifcation(not1)
receiveNotifcation(not1)
receiveNotifcation(not1)
receiveNotifcation(not1)
import cats.effect
import cats.effect.{ExitCode, IO, IOApp, Ref}
import ru.wdevs.cc1503.MainServ

object Run extends IOApp {
  override def run(args: List[String]): IO[effect.ExitCode] =
    MainServ.run(args)

}

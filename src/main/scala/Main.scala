import cats.effect.{ExitCode, IO, IOApp}
import ru.wdevs.cc1503.MainServ

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    MainServ.run(args)
}

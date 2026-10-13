import java.awt.*;

public class CollisionDetection {

    private Car car;
    private BotCar bot;

    public CollisionDetection(Car car, BotCar bot) {
        this.car = car;
        this.bot = bot;
    }

    ///Checks if the car's rectangle overlaps with the bot's rectangle
    public boolean checkCollision() {
        return car.getBounds().intersects(bot.getBounds());
    }

}

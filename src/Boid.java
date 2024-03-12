import java.awt.*;
import java.util.List;

public class Boid {
    static Point target = new Point();
    Color color;
    DoublePoint position;
    DoublePoint velocity;
    boolean markedForRemoval = false;
    DoublePoint desiredVelocityChange = new DoublePoint();
    public Boid() {
        position = new DoublePoint();
        velocity = new DoublePoint();
    }

    public Boid(DoublePoint p, DoublePoint v) {
        position = p;
        velocity = v;
        color = Color.blue;
    }
    public Boid(DoublePoint p, DoublePoint v,Color c) {
        position = p;
        velocity = v;
        color = c;
    }

    public void updateVelocity(Grid grid){
        DoublePoint avgPos = new DoublePoint();
        DoublePoint avgVelocity = new DoublePoint();
        int boidsInViewRangeAndFriendly = 0;
        desiredVelocityChange.x = 0;
        desiredVelocityChange.y = 0;

        List<Boid> localBoids = grid.getNeighbors(this, BoidSettings.VIEW_RANGE);

        for (Boid checkBoid : localBoids) {
            if (checkBoid == this) {continue;} // ignore if same boid

            boolean difColor = !this.color.equals(checkBoid.color);
            double xDif = (position.x - checkBoid.position.x);
            double yDif = (position.y - checkBoid.position.y);
            double distance = Math.sqrt((xDif * xDif) + ((yDif * yDif)));
            double relativeSpeed = this.velocity.distance(checkBoid.velocity);

            if (relativeSpeed > BoidSettings.BATTLE_SPEED_MIN && distance < BoidSettings.BATTLE_RANGE && difColor) {
                checkBoid.markedForRemoval = true;
                this.markedForRemoval = true;
                return;
            }
            if (distance <= BoidSettings.SEPARATION_RANGE) {
                // Separation
                desiredVelocityChange.translate((xDif* BoidSettings.SEPARATION_FORCE),(yDif* BoidSettings.SEPARATION_FORCE));
            } else if (distance <= BoidSettings.VIEW_RANGE) {
                if (difColor){
                    // Color Separation
                    desiredVelocityChange.translate((xDif* BoidSettings.DIF_COLOR_SEP_FORCE),(yDif* BoidSettings.DIF_COLOR_SEP_FORCE));
                }else {
                    // cohesion and alignment helper
                    boidsInViewRangeAndFriendly++;
                    avgPos.translate(checkBoid.position.x, checkBoid.position.y);
                    avgVelocity.translate(checkBoid.velocity.x, checkBoid.velocity.y);
                }
            }
        }

        if (boidsInViewRangeAndFriendly != 0) {
            avgVelocity.x /= boidsInViewRangeAndFriendly;
            avgVelocity.y /= boidsInViewRangeAndFriendly;
            avgPos.x /= boidsInViewRangeAndFriendly;
            avgPos.y /= boidsInViewRangeAndFriendly;

            //cohesion
            if (position.distance(avgPos) > BoidSettings.COHESION_RANGE) {
                desiredVelocityChange.x +=  ((-position.x + avgPos.x) * BoidSettings.COHESION_FORCE);
                desiredVelocityChange.y += ((-position.y + avgPos.y) * BoidSettings.COHESION_FORCE);
            }

            //alignment
            desiredVelocityChange.x +=  ((-velocity.x + avgVelocity.x) * BoidSettings.ALIGNMENT_FORCE);
            desiredVelocityChange.y +=  ((-velocity.y + avgVelocity.y) * BoidSettings.ALIGNMENT_FORCE);
        }

        Target(); // move towards target if in range
        AvoidWall();

        velocity = DoublePoint.lerp(velocity, new DoublePoint(velocity.x + desiredVelocityChange.x, velocity.y + desiredVelocityChange.y), 0.75);

        // drag
        velocity.x /= BoidSettings.DRAG;
        velocity.y /= BoidSettings.DRAG;
        fixSpeed();
    }

    public void update(Grid grid) {
        position.x += velocity.x;
        position.y += velocity.y;
        updateVelocity(grid);
    }
    public void fixSpeed() {
        double magnitude = Math.sqrt((velocity.x * velocity.x) + (velocity.y * velocity.y));
        if (magnitude < BoidSettings.MIN_SPEED || magnitude > BoidSettings.MAX_SPEED) {
            double scale = magnitude < BoidSettings.MIN_SPEED ? BoidSettings.MIN_SPEED / magnitude : BoidSettings.MAX_SPEED / magnitude;
            velocity.x *= scale;
            velocity.y *= scale;
        }
    }

    public void Target(){
        if (target != null && position.distance(target) < BoidSettings.TARGET_MAX_RANGE && position.distance(target) > BoidSettings.TARGET_MIN_RANGE) { // check if close enough to target to move towards
            // Targeting
            desiredVelocityChange.x +=  ((-position.x + target.x) * BoidSettings.TARGET_FORCE);
            desiredVelocityChange.y +=  ((-position.y + target.y) * BoidSettings.TARGET_FORCE);
        }
    }

    public void AvoidWall(){
        if (position.x < BoidSettings.BORDER_MARGIN) {
            velocity.x += BoidSettings.TURN_FACTOR;
        }
        if (position.y < BoidSettings.BORDER_MARGIN) {
            velocity.y += BoidSettings.TURN_FACTOR;
        }
        if (position.x > (BoidFrame.singleton.getWidth()) - BoidSettings.BORDER_MARGIN) {
            velocity.x -= BoidSettings.TURN_FACTOR;
        }
        if (position.y > (BoidFrame.singleton.getHeight()) - BoidSettings.BORDER_MARGIN) {
            velocity.y -= BoidSettings.TURN_FACTOR;
        }
    }


}

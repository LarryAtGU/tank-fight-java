package tankfight.model;

public interface Damageable {
    int getHealth();

    int getMaxHealth();

    boolean isAlive();

    void takeDamage(int amount);
}

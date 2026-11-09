import java.math.BigDecimal;

public class Producto {
    private final int id;
    private final String nombre;
    private final int stock;
    private final int minimo;
    private final BigDecimal precio;

    public Producto(int id, String nombre, int stock, int minimo, BigDecimal precio) {
        this.id = id;
        this.nombre = nombre;
        this.stock = stock;
        this.minimo = minimo;
        this.precio = precio;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public int getStock() { return stock; }
    public int getMinimo() { return minimo; }
    public BigDecimal getPrecio() { return precio; }

    @Override
    public String toString() {
        return id + " | " + nombre + " | $" + precio +
               " | Stock: " + stock + " | Mínimo: " + minimo +
               (stock <= minimo ? " | REPONER" : "");
    }
}

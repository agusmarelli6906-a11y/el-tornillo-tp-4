import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;

public class ProductoDAOImpl implements ProductoDAO {
    private final Connection conexion;

    public ProductoDAOImpl(Connection conexion) {
        this.conexion = conexion;
    }

    @Override
    public Producto buscarProducto(int id, boolean bloquear) throws SQLException {
        String sql = "SELECT id_producto, nombre, stock_actual, stock_minimo, precio " +
                     "FROM producto WHERE id_producto = ?" +
                     (bloquear ? " FOR UPDATE" : "");

        try (PreparedStatement p = conexion.prepareStatement(sql)) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    throw new IllegalArgumentException("Producto inexistente.");
                }
                return convertirProducto(r);
            }
        }
    }

    @Override
    public ArrayList<Producto> listarProductos(boolean soloAlertas) throws SQLException {
        ArrayList<Producto> productos = new ArrayList<>();

        String sql = "SELECT id_producto, nombre, stock_actual, stock_minimo, precio " +
                     "FROM producto" +
                     (soloAlertas ? " WHERE stock_actual <= stock_minimo" : "") +
                     " ORDER BY nombre";

        try (Statement s = conexion.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                productos.add(convertirProducto(r));
            }
        }
        return productos;
    }

    private Producto convertirProducto(ResultSet r) throws SQLException {
        return new Producto(
            r.getInt("id_producto"),
            r.getString("nombre"),
            r.getInt("stock_actual"),
            r.getInt("stock_minimo"),
            r.getBigDecimal("precio")
        );
    }

    @Override
    public void registrarIngreso(int idProducto, int cantidad) throws SQLException {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }

        boolean autocommitAnterior = conexion.getAutoCommit();
        conexion.setAutoCommit(false);

        try {
            Producto producto = buscarProducto(idProducto, true);
            int nuevoStock = producto.getStock() + cantidad;

            try (PreparedStatement p = conexion.prepareStatement(
                    "UPDATE producto SET stock_actual = ? WHERE id_producto = ?")) {
                p.setInt(1, nuevoStock);
                p.setInt(2, idProducto);
                p.executeUpdate();
            }

            conexion.commit();
            System.out.println("Ingreso registrado. Stock actual: " + nuevoStock +
                    (nuevoStock <= producto.getMinimo() ? " (ALERTA: reponer)" : ""));
        } catch (SQLException | RuntimeException e) {
            rollbackSeguro();
            throw e;
        } finally {
            conexion.setAutoCommit(autocommitAnterior);
        }
    }

    @Override
    public void registrarVenta(int idUsuario, int idProducto, int cantidad) throws SQLException {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }

        boolean autocommitAnterior = conexion.getAutoCommit();
        conexion.setAutoCommit(false);

        try {
            Producto producto = buscarProducto(idProducto, true);

            if (producto.getStock() < cantidad) {
                throw new IllegalArgumentException(
                    "Stock insuficiente. Disponible: " + producto.getStock()
                );
            }

            BigDecimal total = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
            int nuevoStock = producto.getStock() - cantidad;

            int idVenta;
            String sqlVenta = "INSERT INTO venta (id_usuario, total_general) VALUES (?, ?)";

            try (PreparedStatement p = conexion.prepareStatement(
                    sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                p.setInt(1, idUsuario);
                p.setBigDecimal(2, total);
                p.executeUpdate();

                try (ResultSet claves = p.getGeneratedKeys()) {
                    if (!claves.next()) {
                        throw new SQLException("No se pudo obtener el ID de la venta.");
                    }
                    idVenta = claves.getInt(1);
                }
            }

            String sqlDetalle = "INSERT INTO detalles_ventas " +
                "(id_venta, id_producto, cantidad, precio_unitario) VALUES (?, ?, ?, ?)";

            try (PreparedStatement p = conexion.prepareStatement(sqlDetalle)) {
                p.setInt(1, idVenta);
                p.setInt(2, idProducto);
                p.setInt(3, cantidad);
                p.setBigDecimal(4, producto.getPrecio());
                p.executeUpdate();
            }

            try (PreparedStatement p = conexion.prepareStatement(
                    "UPDATE producto SET stock_actual = ? WHERE id_producto = ?")) {
                p.setInt(1, nuevoStock);
                p.setInt(2, idProducto);
                p.executeUpdate();
            }

            conexion.commit();
            System.out.println("Venta registrada (ID: " + idVenta + "). Total: $" + total +
                    " | Stock actual: " + nuevoStock +
                    (nuevoStock <= producto.getMinimo() ? " (ALERTA: reponer)" : ""));
        } catch (SQLException | RuntimeException e) {
            rollbackSeguro();
            throw e;
        } finally {
            conexion.setAutoCommit(autocommitAnterior);
        }
    }

    @Override
    public ArrayList<String> listarUsuarios() throws SQLException {
        ArrayList<String> usuarios = new ArrayList<>();

        String sql = "SELECT id_usuario, nombre, rol FROM usuario ORDER BY nombre";
        try (Statement s = conexion.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                usuarios.add(r.getInt("id_usuario") + " | " +
                             r.getString("nombre") + " | " +
                             r.getString("rol"));
            }
        }
        return usuarios;
    }

    private void rollbackSeguro() {
        try {
            conexion.rollback();
        } catch (SQLException e) {
            System.err.println("También ocurrió un error al deshacer la operación: " + e.getMessage());
        }
    }
}

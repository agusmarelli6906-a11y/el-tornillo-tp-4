import java.sql.SQLException;
import java.util.ArrayList;

public interface ProductoDAO {
    Producto buscarProducto(int id, boolean bloquear) throws SQLException;
    ArrayList<Producto> listarProductos(boolean soloAlertas) throws SQLException;
    void registrarIngreso(int idProducto, int cantidad) throws SQLException;
    void registrarVenta(int idUsuario, int idProducto, int cantidad) throws SQLException;
    ArrayList<String> listarUsuarios() throws SQLException;
}

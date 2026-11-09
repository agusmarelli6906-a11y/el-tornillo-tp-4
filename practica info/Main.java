import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    private static final String URL = "jdbc:mysql://localhost:3306/sgiv_tornillo";
    private static final String USER =
        System.getenv().getOrDefault("SGIV_DB_USER", "root");
    private static final String PASS =
        System.getenv().getOrDefault("SGIV_DB_PASSWORD", "12345");

    private static final Scanner entrada = new Scanner(System.in);

    public static void main(String[] args) {
        try (Connection conexion = DriverManager.getConnection(URL, USER, PASS)) {
            ProductoDAO productoDAO = new ProductoDAOImpl(conexion);
            ejecutarMenu(productoDAO);
        } catch (SQLException e) {
            System.err.println("No se pudo conectar o trabajar con MySQL: " + e.getMessage());
        }
    }

    private static void ejecutarMenu(ProductoDAO dao) {
        String[] opcionesMenu = {
            "1. Listar productos",
            "2. Consultar producto por código",
            "3. Registrar venta",
            "4. Registrar ingreso de mercadería",
            "5. Ver alertas de stock",
            "6. Listar usuarios/vendedores",
            "0. Salir"
        };

        int opcion = -1;

        do {
            System.out.println("\n\n========== SGIV-Tornillo ==========\n");
            for (String texto : opcionesMenu) {
                System.out.println(texto);
            }

            try {
                opcion = leerNumero("Opción: ");

                System.out.println("\n");

                switch (opcion) {
                    case 1:
                        mostrarProductos(dao.listarProductos(false));
                        break;
                    case 2:
                        System.out.println(dao.buscarProducto(leerNumero("Código: "), false));
                        break;
                    case 3:
                        mostrarUsuarios(dao.listarUsuarios());
                        int idUsuario = leerNumero("ID Usuario (vendedor): ");
                        int idProductoVenta = leerNumero("Código producto: ");
                        int cantidadVenta = leerNumero("Cantidad: ");
                        dao.registrarVenta(idUsuario, idProductoVenta, cantidadVenta);
                        break;
                    case 4:
                        int idProductoIngreso = leerNumero("Código producto: ");
                        int cantidadIngreso = leerNumero("Cantidad a ingresar: ");
                        dao.registrarIngreso(idProductoIngreso, cantidadIngreso);
                        break;
                    case 5:
                        mostrarProductos(dao.listarProductos(true));
                        break;
                    case 6:
                        mostrarUsuarios(dao.listarUsuarios());
                        break;
                    case 0:
                        System.out.println("Fin del programa.");
                        break;
                    default:
                        System.out.println("Opción inválida.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: debe ingresar un número entero.");
                opcion = -1;
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
                opcion = -1;
            } catch (SQLException e) {
                System.out.println("Error al consultar o actualizar MySQL: " + e.getMessage());
                opcion = -1;
            }
        } while (opcion != 0);
    }

    private static int leerNumero(String pregunta) {
        System.out.print(pregunta);
        return Integer.parseInt(entrada.nextLine().trim());
    }

    private static void mostrarProductos(ArrayList<Producto> productos) {
        if (productos.isEmpty()) {
            System.out.println("Sin productos para mostrar.");
            return;
        }

        for (Producto producto : productos) {
            System.out.println(producto);
        }
    }

    private static void mostrarUsuarios(ArrayList<String> usuarios) {
        System.out.println("Usuarios disponibles:");
        if (usuarios.isEmpty()) {
            System.out.println("No hay usuarios cargados.");
            return;
        }

        for (String usuario : usuarios) {
            System.out.println(usuario);
        }
    }
}

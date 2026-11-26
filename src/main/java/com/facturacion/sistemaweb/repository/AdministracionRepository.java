package com.facturacion.sistemaweb.repository;

import java.util.*;
import java.sql.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.facturacion.sistemaweb.model.UsuarioResumen;
import com.facturacion.sistemaweb.security.CredencialVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;

/** Las lecturas seleccionan columnas explícitas y nunca recuperan CONTRASENIA. */
@Repository @Profile("postgres")
public class AdministracionRepository {
    private final JdbcTemplate jdbc;
    private final CredencialVersionRepository versiones;
    private static final String SELECT = "SELECT u.ID_USUARIO,u.ID_TIPO_USUARIO,u.NOMBRE_USUARIO,t.TIPO_USUARIO,u.ACTIVO FROM dbo.Usuario u JOIN dbo.TipoUsuario t ON t.ID_TIPO_USUARIO=u.ID_TIPO_USUARIO ";
    private static final RowMapper<UsuarioResumen> MAP = (r,n) -> new UsuarioResumen(r.getLong(1),r.getInt(2),r.getString(3),r.getString(4),r.getBoolean(5));
    public AdministracionRepository(JdbcTemplate jdbc) { this(jdbc,null); }
    @Autowired public AdministracionRepository(JdbcTemplate jdbc,CredencialVersionRepository versiones) {
        this.jdbc=jdbc;this.versiones=versiones;
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void bloquear() {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            if (connection.getAutoCommit()) {
                throw new InvalidDataAccessApiUsageException("El bloqueo administrativo requiere una conexión transaccional.");
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pg_advisory_xact_lock(hashtext('SistemaFacturacionWeb.Admin'))")) {
                statement.execute();
            }
            return null;
        });
    }
    public List<UsuarioResumen> buscar(String q, Integer tipo, Boolean activo) {
        return jdbc.query(SELECT + "WHERE (u.NOMBRE_USUARIO LIKE ? OR t.TIPO_USUARIO LIKE ?) AND (cast(? as integer) IS NULL OR u.ID_TIPO_USUARIO=?) AND (cast(? as boolean) IS NULL OR u.ACTIVO=?) ORDER BY u.NOMBRE_USUARIO", MAP,
            "%"+q+"%", "%"+q+"%", tipo,tipo,activo,activo);
    }
    public Optional<UsuarioResumen> buscarId(Long id) { return jdbc.query(SELECT+"WHERE u.ID_USUARIO=?", MAP,id).stream().findFirst(); }
    public Optional<UsuarioResumen> buscarNombre(String nombre) { return jdbc.query(SELECT+"WHERE u.NOMBRE_USUARIO=?", MAP,nombre).stream().findFirst(); }
    public boolean esAdmin(String nombre, String reservado) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Usuario u JOIN dbo.TipoUsuario t ON t.ID_TIPO_USUARIO=u.ID_TIPO_USUARIO WHERE u.NOMBRE_USUARIO=? AND u.ACTIVO=true AND t.TIPO_USUARIO=?", Long.class,nombre,reservado) == 1;
    }
    public boolean tipoReservado(Integer id, String reservado) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.TipoUsuario WHERE ID_TIPO_USUARIO=? AND TIPO_USUARIO=?", Long.class,id,reservado) > 0;
    }
    public long administradoresActivos(String reservado) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Usuario u JOIN dbo.TipoUsuario t ON t.ID_TIPO_USUARIO=u.ID_TIPO_USUARIO WHERE u.ACTIVO=true AND t.TIPO_USUARIO=?", Long.class,reservado);
    }
    public boolean nombreDuplicado(String nombre, Long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Usuario WHERE NOMBRE_USUARIO=? AND (cast(? as bigint) IS NULL OR ID_USUARIO<>?)", Long.class,nombre,id,id)>0;
    }
    public long totalUsuarios() { return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Usuario", Long.class); }
    public boolean tipoEnUso(Integer id) { return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Usuario WHERE ID_TIPO_USUARIO=?",Long.class,id)>0; }
    public void insertar(String nombre, Integer tipo, boolean activo, byte[] codificada) {
        jdbc.update("INSERT INTO dbo.Usuario (NOMBRE_USUARIO,ID_TIPO_USUARIO,ACTIVO,CONTRASENIA) VALUES (?,?,?,?)",nombre,tipo,activo,codificada);
    }
    public void editar(Long id,String nombre,Integer tipo) { jdbc.update("UPDATE dbo.Usuario SET NOMBRE_USUARIO=?,ID_TIPO_USUARIO=? WHERE ID_USUARIO=?",nombre,tipo,id); }
    public void estado(Long id,boolean activo) { jdbc.update("UPDATE dbo.Usuario SET ACTIVO=? WHERE ID_USUARIO=?",activo,id); }
    /** Inserta la cuenta y devuelve su ID_USUARIO (IDENTITY) mediante getGeneratedKeys. */
    public Long insertarDevolviendoId(String nombre, Integer tipo, boolean activo, byte[] codificada) {
        var claves=new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(connection -> {
            String sql = "INSERT INTO dbo.Usuario (NOMBRE_USUARIO,ID_TIPO_USUARIO,ACTIVO,CONTRASENIA) VALUES (?,?,?,?)";
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id_usuario"});
            statement.setString(1,nombre);statement.setInt(2,tipo);statement.setBoolean(3,activo);statement.setBytes(4,codificada);
            return statement;
        },claves);
        Number id=claves.getKey();
        if(id==null) throw new DataAccessResourceFailureException("La base de datos no devolvió el identificador del usuario creado.");
        return id.longValue();
    }
    public void clave(Long id,byte[] codificada) {
        if(versiones!=null) versiones.bloquearCuenta(id);
        if(jdbc.update("UPDATE dbo.Usuario SET CONTRASENIA=? WHERE ID_USUARIO=?",codificada,id)==1 && versiones!=null)
            versiones.incrementarYRevocar(id);
    }
}

package com.facturacion.sistemaweb.repository;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import com.facturacion.sistemaweb.service.AdministracionException;
import com.facturacion.sistemaweb.security.CredencialVersionRepository;

@Repository @Profile("postgres & recuperar-admin & !bootstrap-admin") @ConditionalOnNotWebApplication
public class RecuperacionAdminRepository {
    public record Cuenta(long id,int tipo,String nombre,boolean activa) {
        public String nombreVisible() { return nombre.stripTrailing(); }
    }
    private final JdbcTemplate jdbc;
    private final CredencialVersionRepository versiones;
    public RecuperacionAdminRepository(JdbcTemplate jdbc) { this(jdbc,null); }
    @Autowired public RecuperacionAdminRepository(JdbcTemplate jdbc,CredencialVersionRepository versiones) {
        this.jdbc=jdbc;this.versiones=versiones;
    }
    public void exigirPermisos() {
        Integer permitido=jdbc.queryForObject("SELECT CASE WHEN session_user=current_user AND "
            +"(pg_has_role(current_user, datdba, 'USAGE') OR EXISTS "
            +"(SELECT 1 FROM pg_roles WHERE rolname=current_user AND rolsuper)) THEN 1 ELSE 0 END "
            +"FROM pg_database WHERE datname=current_database()",Integer.class);
        if(!Integer.valueOf(1).equals(permitido)) throw new AdministracionException(
            "La recuperación local requiere el propietario de la base o un superusuario de PostgreSQL, sin cambio de rol.");
    }
    public void exigirSinTriggers() {
        Long cantidad=jdbc.queryForObject("SELECT COUNT(*) FROM pg_trigger WHERE tgrelid='dbo.usuario'::regclass AND NOT tgisinternal AND tgenabled<>'D'",Long.class);
        if(cantidad==null || cantidad!=0) throw new AdministracionException("La recuperación se detuvo: existen triggers activos en Usuario o no se pudo comprobar su ausencia.");
    }
    private static final String SELECT="SELECT u.ID_USUARIO,u.ID_TIPO_USUARIO,u.NOMBRE_USUARIO,u.ACTIVO FROM dbo.Usuario u ";
    public List<Cuenta> candidatas(String reservado) {
        return jdbc.query(SELECT+"JOIN dbo.TipoUsuario t ON t.ID_TIPO_USUARIO=u.ID_TIPO_USUARIO WHERE t.TIPO_USUARIO=? AND u.ACTIVO=true ORDER BY u.ID_USUARIO",
            (r,n)->new Cuenta(r.getLong(1),r.getInt(2),r.getString(3),r.getBoolean(4)),reservado);
    }
    public Cuenta bloquearCuenta(long id,String reservado) {
        var filas=jdbc.query(SELECT+"JOIN dbo.TipoUsuario t ON t.ID_TIPO_USUARIO=u.ID_TIPO_USUARIO "
            +"WHERE u.ID_USUARIO=? AND t.TIPO_USUARIO=? AND u.ACTIVO=true FOR UPDATE OF u,t",
            (r,n)->new Cuenta(r.getLong(1),r.getInt(2),r.getString(3),r.getBoolean(4)),id,reservado);
        if(filas.size()!=1) throw new AdministracionException("Selecciona una única cuenta administradora existente y activa.");
        return filas.getFirst();
    }
    public int actualizarClave(long id,String reservado,byte[] bytes) {
        if(versiones!=null) versiones.bloquearCuenta(id);
        int actualizadas=jdbc.update("UPDATE dbo.Usuario u SET CONTRASENIA=? FROM dbo.TipoUsuario t "
            +"WHERE t.ID_TIPO_USUARIO=u.ID_TIPO_USUARIO AND u.ID_USUARIO=? AND u.ACTIVO=true AND t.TIPO_USUARIO=?",bytes,id,reservado);
        if(actualizadas==1 && versiones!=null) versiones.incrementarYRevocar(id);
        return actualizadas;
    }
}

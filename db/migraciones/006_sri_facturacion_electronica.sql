-- Migración 006: Tablas administrativas y de comprobantes electrónicos SRI (XML, RIDE y Ambiente de Pruebas)
USE [SistemaFacturacionWeb];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID('dbo.EmisorSri', 'U') IS NULL
    BEGIN
        CREATE TABLE dbo.EmisorSri (
            ID_EMISOR INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            RUC VARCHAR(13) NOT NULL,
            RAZON_SOCIAL NVARCHAR(300) NOT NULL,
            NOMBRE_COMERCIAL NVARCHAR(300) NULL,
            DIR_MATRIZ NVARCHAR(300) NOT NULL,
            DIR_ESTABLECIMIENTO NVARCHAR(300) NOT NULL,
            COD_ESTABLECIMIENTO VARCHAR(3) NOT NULL DEFAULT '001',
            PUNTO_EMISION VARCHAR(3) NOT NULL DEFAULT '001',
            OBLIGADO_CONTABILIDAD BIT NOT NULL DEFAULT 0,
            CONTRIBUYENTE_ESPECIAL VARCHAR(20) NULL,
            AMBIENTE SMALLINT NOT NULL DEFAULT 1,
            TIPO_EMISION SMALLINT NOT NULL DEFAULT 1,
            URL_RECEPCION VARCHAR(300) NOT NULL DEFAULT 'https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline',
            URL_AUTORIZACION VARCHAR(300) NOT NULL DEFAULT 'https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline',
            RUTA_CERTIFICADO NVARCHAR(500) NULL,
            CLAVE_CERTIFICADO NVARCHAR(200) NULL,
            ACTIVO BIT NOT NULL DEFAULT 1
        );

        INSERT INTO dbo.EmisorSri (
            RUC, RAZON_SOCIAL, NOMBRE_COMERCIAL, DIR_MATRIZ, DIR_ESTABLECIMIENTO,
            COD_ESTABLECIMIENTO, PUNTO_EMISION, OBLIGADO_CONTABILIDAD, AMBIENTE, TIPO_EMISION,
            URL_RECEPCION, URL_AUTORIZACION, ACTIVO
        ) VALUES (
            '0999999999001',
            N'EMPRESA DEMO FACTURACION S.A.',
            N'SISTEMA FACTURACION WEB',
            N'Av. Quito y Calle 12, Quevedo, Los Rios',
            N'Av. Quito y Calle 12, Quevedo, Los Rios',
            '001',
            '001',
            0,
            1,
            1,
            'https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline',
            'https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline',
            1
        );
    END;

    IF OBJECT_ID('dbo.FacturaSri', 'U') IS NULL
    BEGIN
        CREATE TABLE dbo.FacturaSri (
            ID_FACTURA_SRI BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            ID_FACTURA BIGINT NOT NULL UNIQUE,
            AMBIENTE SMALLINT NOT NULL,
            ESTABLECIMIENTO VARCHAR(3) NOT NULL,
            PUNTO_EMISION VARCHAR(3) NOT NULL,
            SECUENCIAL VARCHAR(9) NOT NULL,
            CLAVE_ACCESO VARCHAR(49) NOT NULL,
            NUMERO_AUTORIZACION VARCHAR(49) NULL,
            FECHA_AUTORIZACION DATETIME2 NULL,
            ESTADO_SRI VARCHAR(30) NOT NULL,
            EXITOSO_PRUEBAS BIT NOT NULL DEFAULT 0,
            MENSAJE_RETORNO NVARCHAR(MAX) NULL,
            XML_COMPROBANTE NVARCHAR(MAX) NOT NULL,
            XML_RESPUESTA_SRI NVARCHAR(MAX) NULL,
            FECHA_ENVIO DATETIME2 NULL,
            CONSTRAINT FK_FacturaSri_Factura FOREIGN KEY (ID_FACTURA)
                REFERENCES dbo.Factura (ID_FACTURA)
        );
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

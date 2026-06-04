/* ============================================================
   NotifySync — SQL Server schema (manual DDL)
   Mirrors the EF Core model. For app-managed schema, prefer
   `dotnet ef database update`; this script is for DBAs / manual setup.
   ============================================================ */

IF DB_ID('NotifySync') IS NULL
    CREATE DATABASE NotifySync;
GO
USE NotifySync;
GO

IF OBJECT_ID('dbo.Notifications', 'U') IS NOT NULL DROP TABLE dbo.Notifications;
IF OBJECT_ID('dbo.NotificationSettings', 'U') IS NOT NULL DROP TABLE dbo.NotificationSettings;
IF OBJECT_ID('dbo.Devices', 'U') IS NOT NULL DROP TABLE dbo.Devices;
IF OBJECT_ID('dbo.Users', 'U') IS NOT NULL DROP TABLE dbo.Users;
GO

CREATE TABLE dbo.Users (
    Id            INT IDENTITY(1,1) PRIMARY KEY,
    Email         NVARCHAR(256) NOT NULL,
    PasswordHash  NVARCHAR(512) NOT NULL,
    FirstName     NVARCHAR(100) NOT NULL,
    LastName      NVARCHAR(100) NOT NULL,
    Role          NVARCHAR(50)  NOT NULL CONSTRAINT DF_Users_Role DEFAULT 'User',
    CreatedAt     DATETIME2     NOT NULL CONSTRAINT DF_Users_CreatedAt DEFAULT SYSUTCDATETIME()
);
CREATE UNIQUE INDEX IX_Users_Email ON dbo.Users(Email);
GO

CREATE TABLE dbo.Devices (
    Id               INT IDENTITY(1,1) PRIMARY KEY,
    UserId           INT NOT NULL,
    DeviceName       NVARCHAR(150) NOT NULL,
    DeviceIdentifier NVARCHAR(200) NOT NULL,
    LastSeen         DATETIME2 NULL,
    CreatedAt        DATETIME2 NOT NULL CONSTRAINT DF_Devices_CreatedAt DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Devices_Users FOREIGN KEY (UserId) REFERENCES dbo.Users(Id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX IX_Devices_User_Identifier ON dbo.Devices(UserId, DeviceIdentifier);
GO

CREATE TABLE dbo.Notifications (
    Id               INT IDENTITY(1,1) PRIMARY KEY,
    UserId           INT NOT NULL,
    DeviceId         INT NOT NULL,
    AppName          NVARCHAR(200) NOT NULL,
    PackageName      NVARCHAR(200) NOT NULL,
    Title            NVARCHAR(500) NULL,
    Message          NVARCHAR(4000) NULL,
    NotificationTime DATETIME2 NOT NULL,
    CreatedAt        DATETIME2 NOT NULL CONSTRAINT DF_Notifications_CreatedAt DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Notifications_Users FOREIGN KEY (UserId) REFERENCES dbo.Users(Id) ON DELETE CASCADE,
    CONSTRAINT FK_Notifications_Devices FOREIGN KEY (DeviceId) REFERENCES dbo.Devices(Id)
);
CREATE INDEX IX_Notifications_User_Created ON dbo.Notifications(UserId, CreatedAt);
CREATE INDEX IX_Notifications_AppName ON dbo.Notifications(AppName);
GO

CREATE TABLE dbo.NotificationSettings (
    Id            INT IDENTITY(1,1) PRIMARY KEY,
    UserId        INT NOT NULL,
    IsSyncEnabled BIT NOT NULL CONSTRAINT DF_NS_IsSyncEnabled DEFAULT 1,
    CreatedAt     DATETIME2 NOT NULL CONSTRAINT DF_NS_CreatedAt DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_NS_Users FOREIGN KEY (UserId) REFERENCES dbo.Users(Id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX IX_NS_User ON dbo.NotificationSettings(UserId);
GO

PRINT 'NotifySync schema created successfully.';

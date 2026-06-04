# Root Dockerfile for Render (builds the ASP.NET Core API).
# Render uses repo root as the build context and ./Dockerfile by default.
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src
COPY backend/ ./
RUN dotnet restore NotifySync.sln
RUN dotnet publish src/NotifySync.Api/NotifySync.Api.csproj -c Release -o /app/publish /p:UseAppHost=false

FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS final
WORKDIR /app
COPY --from=build /app/publish .
# Render routes to port 10000 by default.
EXPOSE 10000
ENV ASPNETCORE_URLS=http://+:10000
ENTRYPOINT ["dotnet", "NotifySync.Api.dll"]

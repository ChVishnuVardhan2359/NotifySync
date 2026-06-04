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
# Fixes SIGSEGV (exit 139) on restricted container hosts (seccomp blocks .NET's W^X JIT),
# and uses the workstation GC which is friendlier to small (512 MB) free instances.
ENV DOTNET_EnableWriteXorExecute=0
ENV DOTNET_gcServer=0
# .NET 8 SIGSEGVs in Render-free's gVisor sandbox when probing CPU SIMD features.
# Disable hardware intrinsics + tiered JIT to use the safe software paths.
ENV DOTNET_EnableHWIntrinsic=0
ENV DOTNET_TieredCompilation=0
ENV DOTNET_TieredPGO=0
ENTRYPOINT ["dotnet", "NotifySync.Api.dll"]

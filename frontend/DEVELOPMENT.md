# Frontend Development Configuration

## Proxy Settings

### Current Configuration
- **Proxy Target**: `http://localhost:8080`
- **Purpose**: Direct proxy to backend server for development efficiency

### Design Philosophy
現在の設計は開発環境での即応性を優先しています。

### Future Roadmap
将来的には「マルチコンテナ化」イシューにて以下への移行を予定：

- **Container Separation**: frontend・backendを独立したコンテナとして分離
- **Microservice Architecture**: マイクロサービスアーキテクチャへの移行
- **Dynamic Configuration**: 環境変数ベースの動的プロキシ設定
- **Environment Flexibility**: 本番環境・ステージング環境での柔軟な設定

### Current Scope
本proxy設定は現在のイシューの対象外です。

## Development Setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Start development server:
   ```bash
   npm start
   ```

3. Backend server should be running on `http://localhost:8080`

## Notes
- The proxy configuration in `package.json` is intentionally hardcoded for development simplicity
- Production deployments will use different routing strategies
- See future "Multi-Container" issue for planned architectural improvements

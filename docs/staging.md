# Staging tại nhà

Staging chạy trên máy dev của chủ dự án (Windows, Docker Desktop) và mở ra Internet qua Cloudflare Tunnel tại **https://plandi-staging.evts.id.vn**. Máy tắt hoặc ngủ thì link chết. Chỉ dùng dữ liệu giả.

```
Điện thoại ──HTTPS──▶ Cloudflare ──tunnel──▶ cloudflared ──▶ app:8080 ──▶ postgres, redis
                                     (mọi container nằm trong compose.prod.yml)
```

- Không mở port nào của router. `cloudflared` tự gọi ra Cloudflare.
- Postgres và Redis không có `ports`, chỉ app thấy được.
- App mở `127.0.0.1:8081` để tự kiểm tra trên máy, máy khác trong mạng không vào được.

## Vì sao là `plandi-staging` mà không phải `staging.plandi`

Chứng chỉ HTTPS miễn phí của Cloudflare (Universal SSL) chỉ phủ tên miền gốc và tên miền con một cấp (`*.evts.id.vn`). `staging.plandi.evts.id.vn` là cấp hai, phải trả tiền mới có chứng chỉ. Bản chính thức sau này dùng `plandi.evts.id.vn`.

## Cài lần đầu (chủ dự án làm)

1. Tạo tài khoản Cloudflare miễn phí tại dash.cloudflare.com.
2. **Add a domain** → nhập `evts.id.vn` → chọn gói **Free**. Cloudflare quét các bản ghi DNS cũ. Trang cũ đã dừng nên có thể xóa các bản ghi trỏ về VPS cũ.
3. Cloudflare đưa hai nameserver dạng `xxx.ns.cloudflare.com`. Vào trang quản lý tên miền ở nhà đăng ký, thay hai nameserver hiện tại (`hoalu.vclouddns.com`, `ninhbinh.vclouddns.com`) bằng hai cái của Cloudflare. Chờ Cloudflare báo **Active**, thường vài giờ, có khi tới 24–48 giờ.
4. Vào **Zero Trust → Networks → Tunnels → Create a tunnel** → chọn **Cloudflared** → đặt tên `plandi-staging` → ở bước cài đặt chọn **Docker** (không chọn Windows: connector phải chạy trong compose mới gọi được `app:8080`).
5. Bấm nút **Copy** cạnh lệnh Docker, dán cả lệnh vào Notepad, lấy chuỗi `eyJ…` sau `--token` và dán vào `CLOUDFLARE_TUNNEL_TOKEN=` trong `.env` **trên một dòng**. Token dài khoảng 250 ký tự; bôi đen bằng tay dễ bị thiếu hoặc bị tách dòng.
6. Chạy `docker compose -f compose.prod.yml up -d cloudflared`, chờ tunnel hiện **Healthy**. Log đúng có dòng `Registered tunnel connection`; `Tunnel not found` nghĩa là token của tunnel đã bị xóa.
7. Ở mục **Public Hostname** của tunnel: Subdomain `plandi-staging`, Domain `evts.id.vn`, URL `http://app:8080` (phải có `http://`).
8. Trong Docker Desktop: **Settings → General → Start Docker Desktop when you sign in**, để staging tự chạy lại sau khi khởi động máy.

## Deploy

```bash
./deploy.sh          # git pull → build image → docker compose up -d
```

Kiểm tra:

```bash
curl http://127.0.0.1:8081/actuator/health              # trên máy
curl https://plandi-staging.evts.id.vn/actuator/health  # từ điện thoại dùng 4G
docker compose -f compose.prod.yml logs -f app cloudflared
```

## Cấp quyền ADMIN

Không có giao diện cấp quyền. Đăng ký tài khoản như người dùng thường, rồi chạy:

```bash
docker compose -f compose.prod.yml exec postgres   psql -U plandi -d plandi -c "UPDATE account SET role = 'ADMIN' WHERE email = 'email-cua-ban@example.com';"
```

Đăng xuất rồi đăng nhập lại để phiên mới mang quyền ADMIN. Quyền này dùng để tạo mã đặt lại mật khẩu (`POST /api/v1/admin/password-resets`) khi người dùng nhắn qua Zalo.

## Ghi chú

- Mật khẩu Postgres và `PLANDI_DATA_KEY` trong `.env` được sinh ngẫu nhiên khi tạo file. Mất `.env` thì phải xóa volume `pgdata` để tạo DB mới, vì đây là dữ liệu giả.
- Web hiện chỉ có trang `/`. Khi web có nhiều route (ví dụ link mời `/invite/...`), backend cần trả `index.html` cho các đường dẫn đó. Việc này làm ở Task 16.

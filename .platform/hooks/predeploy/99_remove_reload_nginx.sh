#!/bin/bash

timestamp() {
    date "+%Y-%m-%d %H:%M:%S.%3N"
}

REMOVE_LINES=("client_max_body_size" "gzip")
IS_ERROR=0

for line in "${REMOVE_LINES[@]}"; do
    if ! sudo sed -i "/$line/d" /etc/nginx/nginx.conf; then
        IS_ERROR=1
        break
    fi
done

if [ $IS_ERROR -eq 0 ]; then
    if sudo systemctl reload nginx; then
        echo "$(timestamp) / SUCCESS - nginx 확장 옵션을 성공적으로 적용했습니다."
    else
        echo "$(timestamp) / ERROR - nginx 확장 옵션을 적용하지 못했습니다."
    fi
else
    echo "$(timestamp) / ERROR - nginx 확장 옵션을 적용하지 못했습니다."
fi
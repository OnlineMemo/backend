#!/bin/bash

timestamp() {
    date "+%Y-%m-%d %H:%M:%S.%3N"
}

if pkill -f "onlinememo-backend.jar"; then
    echo "* $(timestamp) / SUCCESS - Spring 프로세스를 성공적으로 종료했습니다."
else
    echo "* $(timestamp) / WARN - 실행 중인 Spring 프로세스가 없어 종료하지 못했습니다."
fi
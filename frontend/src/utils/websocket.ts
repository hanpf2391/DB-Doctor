/**
 * WebSocket 客户端工具类
 *
 * 用于接收实时告警通知
 *
 * @author DB-Doctor
 * @version 3.2.0
 */

export interface AlertMessage {
  id: number
  ruleId: number
  ruleName: string
  severity: string
  status: string
  message: string
  triggeredAt: string
}

type AlertHandler = (alert: AlertMessage) => void

export class AlertWebSocket {
  private ws: WebSocket | null = null
  private reconnectTimer: number | null = null
  private handlers: AlertHandler[] = []
  private url: string = ''
  private reconnectAttempts: number = 0
  private maxReconnectAttempts: number = 10

  /**
   * 连接 WebSocket
   */
  connect(url: string) {
    this.url = url
    this.disconnect()

    try {
      this.ws = new WebSocket(url)

      this.ws.onopen = () => {
        console.log('[WebSocket] 连接成功')
        this.reconnectAttempts = 0

        // 启动心跳
        this.startHeartbeat()
      }

      this.ws.onmessage = (event) => {
        try {
          const alert: AlertMessage = JSON.parse(event.data)
          console.log('[WebSocket] 收到告警:', alert)

          // 通知所有处理器
          this.handlers.forEach(handler => {
            try {
              handler(alert)
            } catch (e) {
              console.error('[WebSocket] 处理器执行失败:', e)
            }
          })
        } catch (e) {
          console.error('[WebSocket] 解析消息失败:', event.data, e)
        }
      }

      this.ws.onclose = () => {
        console.log('[WebSocket] 连接断开')
        this.scheduleReconnect()
      }

      this.ws.onerror = (error) => {
        console.error('[WebSocket] 错误:', error)
      }
    } catch (e) {
      console.error('[WebSocket] 创建连接失败:', e)
      this.scheduleReconnect()
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  /**
   * 注册告警处理器
   */
  onAlert(handler: AlertHandler) {
    this.handlers.push(handler)

    // 返回取消注册函数
    return () => {
      this.handlers = this.handlers.filter(h => h !== handler)
    }
  }

  /**
   * 安排重连
   */
  private scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[WebSocket] 超过最大重连次数，放弃重连')
      return
    }

    this.reconnectAttempts++

    // 指数退避：1s, 2s, 4s, 8s, ... 最大 30s
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts - 1), 30000)

    console.log(`[WebSocket] ${delay / 1000}秒后尝试第 ${this.reconnectAttempts} 次重连`)

    this.reconnectTimer = window.setTimeout(() => {
      this.connect(this.url)
    }, delay)
  }

  /**
   * 启动心跳
   */
  private startHeartbeat() {
    const heartbeatInterval = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send('ping')
      } else {
        clearInterval(heartbeatInterval)
      }
    }, 30000) // 每 30 秒发送一次心跳
  }

  /**
   * 发送消息
   */
  send(message: string) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(message)
    }
  }

  /**
   * 获取连接状态
   */
  getReadyState(): number {
    return this.ws?.readyState ?? WebSocket.CLOSED
  }

  /**
   * 是否已连接
   */
  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }
}

// 创建全局 WebSocket 实例
const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
const wsHost = window.location.host
const wsUrl = `${wsProtocol}//${wsHost}/ws/alerts`

export const alertWebSocket = new AlertWebSocket()

// 自动连接
export function initAlertWebSocket() {
  alertWebSocket.connect(wsUrl)
}

// 监听页面可见性变化，页面可见时重新连接
if (typeof document !== 'undefined') {
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden && !alertWebSocket.isConnected()) {
      console.log('[WebSocket] 页面可见，重新连接')
      alertWebSocket.connect(wsUrl)
    }
  })
}

export default alertWebSocket

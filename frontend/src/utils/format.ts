/**
 * 格式化工具类
 * 统一处理数据显示格式
 */

/**
 * 格式化秒数为3位小数
 * @param seconds 秒数
 * @returns 格式化后的字符串 (例如: "3.142s")
 */
export function formatSeconds(seconds: number | string | undefined | null): string {
  if (seconds === undefined || seconds === null || seconds === '') {
    return '0.000s'
  }

  const num = typeof seconds === 'string' ? parseFloat(seconds) : seconds

  if (isNaN(num)) {
    return '0.000s'
  }

  return `${num.toFixed(3)}s`
}

/**
 * 格式化毫秒数为3位小数（保留 ms 单位）
 * @param milliseconds 毫秒数
 * @returns 格式化后的字符串 (例如: "123.456ms")
 */
export function formatMilliseconds(milliseconds: number | string | undefined | null): string {
  if (milliseconds === undefined || milliseconds === null || milliseconds === '') {
    return '0.000ms'
  }

  const num = typeof milliseconds === 'string' ? parseFloat(milliseconds) : milliseconds

  if (isNaN(num)) {
    return '0.000ms'
  }

  return `${num.toFixed(3)}ms`
}

/**
 * 格式化数字，保留指定小数位数
 * @param num 数字
 * @param decimals 小数位数，默认3位
 * @returns 格式化后的字符串
 */
export function formatNumber(
  num: number | string | undefined | null,
  decimals: number = 3
): string {
  if (num === undefined || num === null || num === '') {
    return `0.${'0'.repeat(decimals)}`
  }

  const number = typeof num === 'string' ? parseFloat(num) : num

  if (isNaN(number)) {
    return `0.${'0'.repeat(decimals)}`
  }

  return number.toFixed(decimals)
}

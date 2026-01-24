package com.dbdoctor.common.exception;

/**
 * AI 分析异常
 * 当 AI 分析失败时抛出此异常
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
public class AiAnalysisException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public AiAnalysisException(String message) {
        super(1003, message);
    }

    public AiAnalysisException(String message, Throwable cause) {
        super(1003, message, cause);
    }
}

package zhttp.service.server

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandler, ChannelHandlerContext}
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerKeepAliveHandler => JHttpServerKeepAliveHandler}
import io.netty.handler.ssl.SslHandler
import zhttp.service.Server.Config
import zhttp.service._

import java.util

case class EncryptedMessageFilter(reqHandler: ChannelHandler, cfg: Config[_, Throwable]) extends ByteToMessageDecoder {
  override def decode(context: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (SslHandler.isEncrypted(in)) {
      context.channel().pipeline().remove(SERVER_CLEAR_TEXT_HTTP2_HANDLER)
      context.channel().pipeline().remove(SERVER_CLEAR_TEXT_HTTP2_FALLBACK_HANDLER)
      context
        .channel()
        .pipeline()
        .addLast(HTTP_SERVER_KEEPALIVE_HANDLER, new JHttpServerKeepAliveHandler)
        .addLast(SERVER_OBJECT_AGGREGATOR, new HttpObjectAggregator(cfg.objectAggregator))
        .addLast(HTTP_SERVER_REQUEST_HANDLER, reqHandler)
        .remove(this)
      ()
    } else {
      context.pipeline().remove(this)
      ()
    }
  }
}

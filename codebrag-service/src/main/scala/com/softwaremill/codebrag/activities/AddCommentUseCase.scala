package com.softwaremill.codebrag.activities

import com.softwaremill.codebrag.service.comments.UserReactionService
import com.softwaremill.codebrag.service.followups.FollowupService
import com.softwaremill.codebrag.service.comments.command.IncomingComment
import com.softwaremill.codebrag.common.{Clock, EventBus}
import com.softwaremill.codebrag.domain.reactions.CommentAddedEvent
import com.softwaremill.codebrag.domain.Comment

class AddCommentUseCase(userReactionService: UserReactionService, followupService: FollowupService, eventBus: EventBus) (implicit clock: Clock) {

  type AddCommentResult = Either[String, Comment]

  def addCommentToCommit(implicit newComment: IncomingComment): AddCommentResult = {
    ifCanExecute {
      val addedComment = userReactionService.storeComment(newComment)
      followupService.generateFollowupsForComment(addedComment)
      eventBus.publish(CommentAddedEvent(addedComment))
      Right(addedComment)
    }
  }

  protected def ifCanExecute(block: => AddCommentResult)(implicit comment: IncomingComment): AddCommentResult = {
    block
  }

}
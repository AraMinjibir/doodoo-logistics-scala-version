package domain.models

import java.util.UUID

sealed trait DomainEvent

case class ShipmentCreated(
                            shipmentId: String,
                            senderName: String,
                            trackingNumber: Option[String]
                          ) extends DomainEvent

case class ShipmentStatusChanged(
                                  shipmentId: String,
                                  newStatus: String,
                                  senderEmail: String,
                                  recipientEmail: String
                                ) extends DomainEvent

case class UserCreated(
                      username: String,
                      role: UserRole,
                      status:UserStatus
                      ) extends DomainEvent

case class UserAccountUpdated(
                               userId: UUID,
                               email: String,
                               status: UserStatus
                             ) extends DomainEvent

case class ShipmentAssigned(
                        username: String,
                        shipmentId: UUID,
                           ) extends DomainEvent

case class ShipmentDelivered(
                              shipmentId: String,
                              trackingNumber: String,
                              senderEmail: String
                            ) extends DomainEvent
import { Stack } from "@foodloop/ui";
import { NewListingForm } from "./NewListingForm";

export default async function NewFoodListingPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;

  return (
    <Stack gap="lg" style={{ maxWidth: 480, margin: "48px auto" }}>
      <h1>List surplus food</h1>
      <NewListingForm donorOrgId={id} />
    </Stack>
  );
}

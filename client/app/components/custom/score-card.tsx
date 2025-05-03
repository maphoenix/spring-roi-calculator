import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"; // Base shadcn component

interface ScoreCardProps {
  title: string;
  value: string | number;
  description?: string;
}

export function ScoreCard({ title, value, description }: ScoreCardProps) {
  return (
    <Card className="flex flex-col justify-center min-h-[120px]">
      <CardHeader className="pb-2 text-center">
        <CardTitle className="text-sm font-medium">{title}</CardTitle>
        {description && <CardDescription>{description}</CardDescription>}
      </CardHeader>
      <CardContent className="text-center">
        <div className="text-2xl font-bold">{value}</div>
      </CardContent>
    </Card>
  );
}
